package com.tomvd.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tomvd.configuration.DevicesConfiguration;
import com.tomvd.configuration.MQTTConfiguration;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

@Singleton
public class HomeAssistantService implements ApplicationService {
    private static final Logger LOG = LoggerFactory.getLogger(HomeAssistantService.class);
    private IMqttClient haClient;
    private final ObjectMapper objectMapper;
    private final DevicesConfiguration devicesConfiguration;
    private final MQTTConfiguration mqttConfig;
    private ServiceLocator sl;

    private final String targetTopic = "ecoflow/powerstream";

    @Inject
    public HomeAssistantService(DevicesConfiguration devicesConfiguration, MQTTConfiguration mqttConfig) {
        this.objectMapper = new ObjectMapper();
        this.devicesConfiguration = devicesConfiguration;
        this.mqttConfig = mqttConfig;
    }

    @Override
    public void setSl(ServiceLocator sl) {
        this.sl = sl;
    }

    @EventListener
    public void onStartup(StartupEvent event) {
        try {
            String mqttClientId = "psbridge";
            haClient = new MqttClient(mqttConfig.getHomeAssistant().getUrl(), mqttClientId);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(mqttConfig.getHomeAssistant().getUsername());
            options.setPassword(mqttConfig.getHomeAssistant().getPassword() != null ? mqttConfig.getHomeAssistant().getPassword().toCharArray() : null);

            haClient.setCallback(new MqttCallback() {

                @Override
                public void connectionLost(Throwable throwable) {
                    LOG.info("Disconnected from target broker: {}", throwable.toString());
                }

                @Override
                public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                    handlePowerMessage(mqttMessage);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

                }
            });
            haClient.connect(options);
            haClient.subscribe(targetTopic+"/setpower", 1);
            if (haClient.isConnected()) {
                if (mqttConfig.isEnableDiscovery()) {
                    publishHomeAssistantDiscovery();
                }
            }
        } catch (MqttException e) {
            LOG.error("Error starting MQTT bridge", e);
        }
    }

    private void handlePowerMessage(MqttMessage mqttMessage) throws MqttException {
        String str = new String(mqttMessage.getPayload(), StandardCharsets.UTF_8);
        int value = Integer.parseInt(str);
        if (value >= 0 && value < 800) {
            sl.getDeviceService().publishPowerSetting(value);
        }
    }

    private void publishHomeAssistantDiscovery() {
        try {
            String deviceId = devicesConfiguration.getPowerstreams().getFirst();

            // Create Home Assistant discovery message
            ObjectNode discoveryInfo = objectMapper.createObjectNode();
            discoveryInfo.put("state_topic", targetTopic+"/state");
            discoveryInfo.put("qos", 2);

            ObjectNode device = discoveryInfo.putObject("dev");
            device.put("ids", deviceId);
            device.put("name", "PowerStream1");
            device.put("mf", "EcoFlow");
            device.put("mdl", "PowerStream");
            device.put("sw", ""); // TODO
            device.put("sn", deviceId);
            device.put("hw", ""); // TODO

            // Add origin information
            ObjectNode origin = discoveryInfo.putObject("o");
            origin.put("name", "psbridge");
            origin.put("sw", ""); // TODO
            origin.put("url", "https://github.com/tomvd/local-powerstream");

            // Add the description of the components within this device
            ObjectNode components = discoveryInfo.putObject("cmps");
            ObjectNode cmp0 = components.putObject("SetOutputWatts");
            cmp0.put("p", "number");
            cmp0.put("command_topic", targetTopic+"/setpower");
            cmp0.put("state_topic", targetTopic+"/state");
            cmp0.put("value_template", "{{ value_json.permanentWatts}}");
            cmp0.put("device_class", "power");
            cmp0.put("unit_of_measurement", "W");
            cmp0.put("min", 0);
            cmp0.put("max", 800);
            cmp0.put("unique_id", "ps1_power_set");
            cmp0.put("mode", "slider");
            cmp0.put("name", "SetOutputWatts");


            ObjectNode cmp1 = components.putObject("OutputWatts");
            cmp1.put("p", "sensor");
            cmp1.put("device_class", "power");
            cmp1.put("unit_of_measurement", "W");
            cmp1.put("value_template", "{{ value_json.invOutputWatts}}");
            cmp1.put("unique_id", "ps1_power_out");
            cmp1.put("name", "OutputWatts");

            ObjectNode cmp2 = components.putObject("PermanentWatts");
            cmp2.put("p", "sensor");
            cmp2.put("device_class", "power");
            cmp2.put("unit_of_measurement", "W");
            cmp2.put("value_template", "{{ value_json.permanentWatts}}");
            cmp2.put("unique_id", "ps1_permanent_watts");
            cmp2.put("name", "PermanentWatts");

            ObjectNode cmp3 = components.putObject("LlcTemp");
            cmp3.put("p", "sensor");
            cmp3.put("device_class", "temperature");
            cmp3.put("unit_of_measurement", "°C");
            cmp3.put("value_template", "{{ value_json.llcTemp}}");
            cmp3.put("unique_id", "ps1_llc_temp");

            ObjectNode cmp4 = components.putObject("pv1InputVolt");
            cmp4.put("p", "sensor");
            cmp4.put("device_class", "voltage");
            cmp4.put("unit_of_measurement", "V");
            cmp4.put("value_template", "{{ value_json.pv1InputVolt}}");
            cmp4.put("unique_id", "ps1_pv1_volt");
            cmp4.put("name", "pv1InputVolt");

            ObjectNode cmp5 = components.putObject("pv1InputCur");
            cmp5.put("p", "sensor");
            cmp5.put("device_class", "current");
            cmp5.put("unit_of_measurement", "A");
            cmp5.put("value_template", "{{ value_json.pv1InputCur}}");
            cmp5.put("unique_id", "ps1_pv1_cur");
            cmp5.put("name", "pv1InputCur");

            ObjectNode cmp6 = components.putObject("pv2InputVolt");
            cmp6.put("p", "sensor");
            cmp6.put("device_class", "voltage");
            cmp6.put("unit_of_measurement", "V");
            cmp6.put("value_template", "{{ value_json.pv2InputVolt}}");
            cmp6.put("unique_id", "ps1_pv2_volt");
            cmp6.put("name", "pv2InputVolt");

            ObjectNode cmp7 = components.putObject("pv2InputCur");
            cmp7.put("p", "sensor");
            cmp7.put("device_class", "current");
            cmp7.put("unit_of_measurement", "A");
            cmp7.put("value_template", "{{ value_json.pv2InputCur}}");
            cmp7.put("unique_id", "ps1_pv2_cur");
            cmp7.put("name", "pv2InputCur");

            // Discovery topic format: homeassistant/device/HWxxx/config
            String discoveryTopic = String.format("homeassistant/device/%s/config", deviceId);

            // Publish discovery information
            MqttMessage discoveryMessage = new MqttMessage();
            discoveryMessage.setPayload(objectMapper.writeValueAsBytes(discoveryInfo));
            discoveryMessage.setQos(1);
            discoveryMessage.setRetained(true);

            haClient.publish(discoveryTopic, discoveryMessage);

        } catch (Exception e) {
            LOG.error("Error publishing Home Assistant discovery information", e);
        }
    }

    @Override
    public boolean isOnline() {
        return haClient.isConnected();
    }

    @Override
    public void publishJsonState(String json) throws MqttException {
        // Publish to target broker
        MqttMessage targetMessage = new MqttMessage();
        targetMessage.setPayload(json.getBytes(StandardCharsets.UTF_8));
        targetMessage.setQos(1);
        targetMessage.setRetained(true);

        haClient.publish(targetTopic + "/state", targetMessage);
    }
}
