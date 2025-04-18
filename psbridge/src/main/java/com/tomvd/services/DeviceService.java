package com.tomvd.services;

import org.eclipse.paho.client.mqttv3.MqttException;

public interface DeviceService {
    void publishPowerSetting(int i) throws MqttException;
    void setSl(ServiceLocator sl);
}
