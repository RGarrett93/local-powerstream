package com.tomvd.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tomvd.configuration.DevicesConfiguration;
import com.tomvd.configuration.MQTTConfiguration;
import com.tomvd.converter.ProtobufConverter;
import com.tomvd.model.PowerStreamData;
import com.tomvd.psbridge.InverterHeartbeat;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Objects;

@Singleton
public class EcoflowService implements DeviceService {
    private static final Logger LOG = LoggerFactory.getLogger(EcoflowService.class);
    private IMqttClient ecoflowClient;
    private final ObjectMapper objectMapper;
    private final ProtobufConverter converter;
    private final DevicesConfiguration devicesConfiguration;
    private final MQTTConfiguration mqttConfig;
    private ServiceLocator sl;

    private String upstreamTopic;
    private String commandTopic;
    private String batteryTopic;

    private PowerStreamData data;

    @Inject
    public EcoflowService(ProtobufConverter converter, DevicesConfiguration devicesConfiguration, MQTTConfiguration mqttConfig) {
        this.objectMapper = new ObjectMapper();
        this.converter = converter;
        this.devicesConfiguration = devicesConfiguration;
        this.mqttConfig = mqttConfig;
        if (!devicesConfiguration.getPowerstreams().isEmpty()) {
            upstreamTopic = "/sys/75/" + devicesConfiguration.getPowerstreams().getFirst()
                    + "/thing/protobuf/upstream";
            commandTopic = "/sys/75/" + devicesConfiguration.getPowerstreams().getFirst()
                    + "/thing/property/cmd";
            batteryTopic = "/sys/72/" + devicesConfiguration.getBatteries().getFirst()
                    + "/thing/property/post";
        }
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    @Override
    public void setSl(ServiceLocator sl) {
        this.sl = sl;
    }

    @Override
    public PowerStreamData getPowerStreamData() {return data;}

    @EventListener
    public void onStartup(StartupEvent event) {
        if (upstreamTopic == null) {
            LOG.info("You forgot to configure any powerstream devices in the application.yml?");
        }
        LOG.info("Starting MQTT bridge");
        try {
            String mqttClientId = "psbridge";
            ecoflowClient = new MqttClient(mqttConfig.getEcoflow().getUrl(), mqttClientId);
            MqttConnectOptions options = getMqttConnectOptions();
            // Set up some callbacks
            ecoflowClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable throwable) {
                    LOG.info("Disconnected from source broker");
                }

                @Override
                public void messageArrived(String topic, MqttMessage mqttMessage) {
                    if (topic.equals(upstreamTopic))
                        handleProtobufMessage(topic, mqttMessage);
                    if (topic.equals(batteryTopic))
                        handleJsonMessage(topic, mqttMessage);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

                }
            });
            ecoflowClient.connect(options);
            ecoflowClient.subscribe(upstreamTopic, 1);
            if (!devicesConfiguration.getBatteries().isEmpty())
                ecoflowClient.subscribe(batteryTopic, 1);
            if (ecoflowClient.isConnected()) {
                publishPowerSetting(0);
            }
        } catch (MqttException e) {
            LOG.error("Error starting MQTT bridge", e);
        }
    }

    private MqttConnectOptions getMqttConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        if (mqttConfig.getEcoflow().getUsername() != null) {
            options.setUserName(mqttConfig.getEcoflow().getUsername());
            if (mqttConfig.getEcoflow().getPassword() != null) {
                options.setPassword(mqttConfig.getEcoflow().getPassword() != null ? mqttConfig.getEcoflow().getPassword().toCharArray() : null);
            }
        }
        options.setSocketFactory(Objects.requireNonNull(getSslContextTrustAll()).getSocketFactory());
        return options;
    }

    @Override
    public void publishPowerSetting(int i) {
        //LOG.info("Publishing to powerstream");
        byte[] payload = converter.getPowerSettingPayload(i, devicesConfiguration.getPowerstreams().getFirst());
        MqttMessage msg = new MqttMessage();
        msg.setPayload(payload);
        msg.setQos(0);
        msg.setRetained(false);
        try {
            ecoflowClient.publish(commandTopic, msg);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    @Scheduled(fixedDelay = "5s")
    void executeHB() throws MqttException {
        if (ecoflowClient != null && ecoflowClient.isConnected() && sl.getApplicationService().isOnline()) {
            publishHB();
        }
    }

    private void publishHB() throws MqttException {
        byte[] payload = converter.convertHB();
        MqttMessage msg = new MqttMessage();
        msg.setPayload(payload);
        msg.setQos(0);
        msg.setRetained(false);
        ecoflowClient.publish(commandTopic, msg);
    }

    private void handleProtobufMessage(String topic, MqttMessage message) {
        try {
            LOG.debug("Received message on topic {}", topic);

            byte[] payload = message.getPayload();
            InverterHeartbeat inverterHeartbeat = converter.convert(payload);
            if (inverterHeartbeat != null && sl.getApplicationService().isOnline()) {
                ObjectNode jsonNode = objectMapper.createObjectNode();

                jsonNode.put("invOutputWatts", inverterHeartbeat.getInvOutputWatts()/10.0);
                jsonNode.put("llcTemp", inverterHeartbeat.getLlcTemp()/10.0);
                jsonNode.put("permanentWatts", inverterHeartbeat.getPermanentWatts()/10.0);
                jsonNode.put("pv1InputVolt", inverterHeartbeat.getPv1InputVolt()/10.0);
                jsonNode.put("pv1InputCur", inverterHeartbeat.getPv1InputCur()/10.0);
                jsonNode.put("pv2InputVolt", inverterHeartbeat.getPv2InputVolt()/10.0);
                jsonNode.put("pv2InputCur", inverterHeartbeat.getPv2InputCur()/10.0);
                jsonNode.put("last_updated", System.currentTimeMillis());

                String json = objectMapper.writeValueAsString(jsonNode);
                sl.getApplicationService().publishJsonState(devicesConfiguration.getPowerstreams().getFirst(), json);

                data = new PowerStreamData(
                        (inverterHeartbeat.getPv1InputVolt()+inverterHeartbeat.getPv2InputVolt())/20.0,
                        inverterHeartbeat.getInvOutputWatts()/10
                );
            }

        } catch (Exception e) {
            LOG.error("Error processing message", e);
        }
    }

    private void handleJsonMessage(String topic, MqttMessage mqttMessage) {
        try {
            JsonNode rootNode = objectMapper.createParser(mqttMessage.getPayload()).readValueAsTree();
            String typeCode = rootNode.get("typeCode").asText();
            switch (typeCode) {
                case "bmsStatus":
                    JsonNode params = rootNode.get("params");
                    ObjectNode jsonNode = objectMapper.createObjectNode();

                    jsonNode.put("soc", params.get("f32ShowSoc").asDouble());
                    jsonNode.put("last_updated", System.currentTimeMillis());

                    String json = objectMapper.writeValueAsString(jsonNode);
                    sl.getApplicationService().publishJsonState(devicesConfiguration.getBatteries().getFirst(), json);
                    break;
            }
        } catch (IOException | MqttException e) {
            throw new RuntimeException(e);
        }
    }

    private static SSLContext getSslContextTrustAll( )
    {
        try
        {
            TrustManager[] trustAllCerts = new TrustManager [] {new X509ExtendedTrustManager() {
                @Override
                public void checkClientTrusted (X509Certificate[] chain, String authType, Socket socket) {

                }

                @Override
                public void checkServerTrusted (X509Certificate [] chain, String authType, Socket socket) {

                }

                @Override
                public void checkClientTrusted (X509Certificate [] chain, String authType, SSLEngine engine) {

                }

                @Override
                public void checkServerTrusted (X509Certificate [] chain, String authType, SSLEngine engine) {

                }

                @Override
                public java.security.cert.X509Certificate [] getAcceptedIssuers () {
                    return null;
                }

                @Override
                public void checkClientTrusted (X509Certificate [] certs, String authType) {
                }

                @Override
                public void checkServerTrusted (X509Certificate [] certs, String authType) {
                }

            }};
            SSLContext sslContext = SSLContext.getInstance( "TLS" );
            sslContext.init( null, trustAllCerts, null );
            return sslContext;
        }
        catch(KeyManagementException | NoSuchAlgorithmException e)
        {
            LOG.error( Thread.currentThread().getStackTrace()[1].getMethodName(), e );
        }
        return null;
    }

}
