package com.tomvd.services;

import com.fasterxml.jackson.core.JsonParser;
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

import java.io.IOException;
import java.util.HashMap;

@Singleton
public class EcoflowService implements DeviceService {
    private static final Logger LOG = LoggerFactory.getLogger(EcoflowService.class);
    private IMqttClient ecoflowClient;
    private final ObjectMapper objectMapper;
    private final ProtobufConverter converter;
    private final DevicesConfiguration devicesConfiguration;
    private final MQTTConfiguration mqttConfig;
    private ServiceLocator sl;
    String batteryTopic;

    private final HashMap<String, PowerStreamData> data;

    @Inject
    public EcoflowService(ProtobufConverter converter, DevicesConfiguration devicesConfiguration, MQTTConfiguration mqttConfig) {
        this.objectMapper = new ObjectMapper();
        this.converter = converter;
        this.devicesConfiguration = devicesConfiguration;
        this.mqttConfig = mqttConfig;
        data = new HashMap<>();
        devicesConfiguration.getPowerstreams().forEach(device -> data.put(device,
                new PowerStreamData(
                        0,
                        0,
                        "/sys/75/"+device+"/thing/protobuf/upstream",
                        "/sys/75/"+device+"/thing/property/cmd"
                        )));
        batteryTopic = "/sys/72/" + devicesConfiguration.getBatteries().getFirst()
                + "/thing/property/post";
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    @Override
    public void setSl(ServiceLocator sl) {
        this.sl = sl;
    }

    @Override
    public PowerStreamData getPowerStreamData() {return data.get(devicesConfiguration.getPowerstreams().getFirst());}

    @EventListener
    public void onStartup(StartupEvent event) {
        if (devicesConfiguration.getPowerstreams().isEmpty()) {
            LOG.info("You forgot to configure any powerstream devices in the application.yml?");
        }
        LOG.info("Starting MQTT bridge");
        try {
            String mqttClientId = "psbridge-ec";
            ecoflowClient = new MqttClient(mqttConfig.getServerUri(), mqttClientId);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(mqttConfig.getUserName());
            options.setPassword(mqttConfig.getPassword() != null ? mqttConfig.getPassword().toCharArray() : null);

            // Set up some callbacks
            ecoflowClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable throwable) {
                    LOG.info("Disconnected from source broker: "+ throwable.toString());
                }

                @Override
                public void messageArrived(String topic, MqttMessage mqttMessage) throws IOException {
                    String[] parts = topic.split("/");
                    String deviceId = parts[3];
                    if (topic.equals(batteryTopic))
                        handleJsonMessage(mqttMessage);
                    else {
                        if (topic.equals(data.get(deviceId).upstreamTopic()))
                            handleProtobufMessage(topic, mqttMessage, deviceId);
                    }

                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                    // do nothing
                }
            });
            ecoflowClient.connect(options);
            devicesConfiguration.getPowerstreams().forEach(device -> {
                try {
                    ecoflowClient.subscribe(data.get(device).upstreamTopic(), 1);
                } catch (MqttException e) {
                    LOG.error("Error starting MQTT bridge", e);
                }
            });
            if (!devicesConfiguration.getBatteries().isEmpty())
                ecoflowClient.subscribe(batteryTopic, 1);
            if (ecoflowClient.isConnected()) {
                publishPowerSetting(0);
            }
        } catch (MqttException e) {
            LOG.error("Error starting MQTT bridge", e);
        }
    }

    @Override
    public void publishPowerSetting(int i) {
        publishPowerSetting(i, devicesConfiguration.getPowerstreams().getFirst());
    }

    @Override
    public void publishPowerSetting(int i, String deviceId) {
        //LOG.info("Publishing to powerstream");
        byte[] payload = converter.getPowerSettingPayload(i, deviceId);
        MqttMessage msg = new MqttMessage();
        msg.setPayload(payload);
        msg.setQos(0);
        msg.setRetained(false);
        try {
            ecoflowClient.publish(data.get(deviceId).commandTopic(), msg);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    @Scheduled(fixedDelay = "5s")
    void executeHeartBeat() {
        if (ecoflowClient != null && ecoflowClient.isConnected() && sl.getApplicationService().isOnline()) {
            devicesConfiguration.getPowerstreams().forEach(device -> {
                try {
                    publishHeartBeat(device);
                } catch (MqttException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private void publishHeartBeat(String deviceId) throws MqttException {
        byte[] payload = converter.convertHeartBeat();
        MqttMessage msg = new MqttMessage();
        msg.setPayload(payload);
        msg.setQos(0);
        msg.setRetained(false);
        ecoflowClient.publish(data.get(deviceId).commandTopic(), msg);
    }

    private void handleProtobufMessage(String topic, MqttMessage message, String deviceId) {
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
                sl.getApplicationService().publishJsonState(deviceId, json);

                data.replace(deviceId, data.get(deviceId).withCurrentPower(inverterHeartbeat.getInvOutputWatts()/10));
                data.replace(deviceId, data.get(deviceId).withAvgVoltage((inverterHeartbeat.getPv1InputVolt()+inverterHeartbeat.getPv2InputVolt())/20.0));
            }

        } catch (Exception e) {
            LOG.error("Error processing message", e);
        }
    }

    private void handleJsonMessage(MqttMessage mqttMessage) throws IOException {
        JsonParser parser = null;
        try {
            parser = objectMapper.createParser(mqttMessage.getPayload());
            JsonNode rootNode = parser.readValueAsTree();
            String typeCode = rootNode.get("typeCode").asText();
            if (typeCode.equals("bmsStatus")) {
                JsonNode params = rootNode.get("params");
                ObjectNode jsonNode = objectMapper.createObjectNode();

                jsonNode.put("soc", params.get("f32ShowSoc").asDouble());
                jsonNode.put("last_updated", System.currentTimeMillis());

                String json = objectMapper.writeValueAsString(jsonNode);
                sl.getApplicationService().publishJsonState(devicesConfiguration.getBatteries().getFirst(), json);
            }
        } catch (IOException | MqttException e) {
            throw new RuntimeException(e);
        } finally {
            if (parser != null) parser.close();
        }
    }

}
