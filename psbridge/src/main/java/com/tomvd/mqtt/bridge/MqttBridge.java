package com.tomvd.mqtt.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tomvd.converter.ProtobufConverter;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Objects;

@Singleton
class MqttBridge {
    private static final Logger LOG = LoggerFactory.getLogger(MqttBridge.class);

    private IMqttClient targetClient;
    private final ObjectMapper objectMapper;
    private final ProtobufConverter converter;


    @Value("${source.mqtt.server:`ssl://192.168.0.111:8883`}")
    private String sourceBrokerUrl;

    private final String mqttClientId = "psbridge";

    @Value("${source.mqtt.topic:/sys/75/HW51xxxxxxxxxxxx/thing/protobuf/upstream}")
    private String sourceTopic;

    @Value("${target.mqtt.server:`tcp://192.168.0.113:1883`}")
    private String targetBrokerUrl;

    @Value("${target.mqtt.username:}")
    private String targetUsername;

    @Value("${target.mqtt.password:}")
    private String targetPassword;

    @Value("${target.mqtt.topic:ecoflow/powerstream}")
    private String targetTopic;

    @Value("${homeassistant.discovery.enabled:false}")
    private boolean haDiscoveryEnabled;

    @Value("${homeassistant.device.name:PSBridge}")
    private String haDeviceName;

    @Value("${homeassistant.device.id:psbridge}")
    private String haDeviceId;

    @Inject
    public MqttBridge(ProtobufConverter converter) {
        this.objectMapper = new ObjectMapper();
        this.converter = converter;
    }

    @EventListener
    public void onStartup(StartupEvent event) {
            LOG.info("Starting MQTT bridge");
            try {
                IMqttClient sourceClient = new MqttClient(sourceBrokerUrl, mqttClientId);
                MqttConnectOptions options = new MqttConnectOptions();
                options.setSocketFactory(Objects.requireNonNull(getSslContextTrustAll()).getSocketFactory());

                // Set up some callbacks
                sourceClient.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable throwable) {
                        LOG.info("Disconnected from source broker");
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                        handleMessage(topic, mqttMessage);
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

                    }
                });
                sourceClient.connect(options);
                sourceClient.subscribe(sourceTopic, 1);
            } catch (MqttException e) {
                LOG.error("Error starting MQTT bridge", e);
            }

            try {
                MqttClient targetClient = new MqttClient(targetBrokerUrl, mqttClientId);
                MqttConnectOptions options = new MqttConnectOptions();
                options.setUserName(targetUsername);
                options.setPassword(targetPassword != null ? targetPassword.toCharArray() : null);

                targetClient.setCallback(new MqttCallback() {

                    @Override
                    public void connectionLost(Throwable throwable) {
                        LOG.info("Disconnected from target broker");
                    }

                    @Override
                    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {

                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

                    }
                });
                targetClient.connect(options);
                if (targetClient.isConnected()) {
                    if (haDiscoveryEnabled) {
                        publishHomeAssistantDiscovery();
                    }
                }
            } catch (MqttException e) {
                LOG.error("Error starting MQTT bridge", e);
            }

    }

    private void handleMessage(String topic, MqttMessage message) {
        try {
            LOG.debug("Received message on topic {}", topic);

            // Get binary payload
            byte[] payload = message.getPayload();

            // Transform the data
            String transformedData = converter.convert(payload);

            // Publish to target broker
            MqttMessage targetMessage = new MqttMessage();
            targetMessage.setPayload(transformedData.getBytes(StandardCharsets.UTF_8));
            targetMessage.setQos(1);
            targetMessage.setRetained(true);

            targetClient.publish(targetTopic, targetMessage);

        } catch (Exception e) {
            LOG.error("Error processing message", e);
        }
    }

    private void publishHomeAssistantDiscovery() {
        try {
            // Create Home Assistant discovery message
            ObjectNode discoveryInfo = objectMapper.createObjectNode();
            discoveryInfo.put("name", haDeviceName);
            discoveryInfo.put("state_topic", targetTopic);
            discoveryInfo.put("unique_id", haDeviceId);

            // Add device information
            ObjectNode device = discoveryInfo.putObject("device");
            device.put("identifiers", haDeviceId);
            device.put("name", haDeviceName);
            device.put("model", "PSBridge");
            device.put("manufacturer", "PSBridge");

            // Set value template based on the JSON structure you're using
            discoveryInfo.put("value_template", "{{ value_json.value }}");

            // Discovery topic format: homeassistant/sensor/psbridge/config
            String discoveryTopic = String.format("homeassistant/sensor/%s/config", haDeviceId);

            // Publish discovery information
            MqttMessage discoveryMessage = new MqttMessage();
            discoveryMessage.setPayload(objectMapper.writeValueAsBytes(discoveryInfo));
            discoveryMessage.setQos(1);
            discoveryMessage.setRetained(true);

            targetClient.publish(discoveryTopic, discoveryMessage);

        } catch (Exception e) {
            LOG.error("Error publishing Home Assistant discovery information", e);
        }
    }

    private static SSLContext getSslContextTrustAll( )
    {
        try
        {
            TrustManager [] trustAllCerts = new TrustManager [] {new X509ExtendedTrustManager() {
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
