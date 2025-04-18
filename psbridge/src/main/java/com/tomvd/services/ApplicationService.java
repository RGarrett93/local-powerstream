package com.tomvd.services;

import org.eclipse.paho.client.mqttv3.MqttException;

public interface ApplicationService {
    void setSl(ServiceLocator sl);
    boolean isOnline();
    void publishJsonState(String json) throws MqttException;
}
