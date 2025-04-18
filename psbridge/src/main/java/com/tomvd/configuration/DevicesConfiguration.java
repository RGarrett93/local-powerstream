package com.tomvd.configuration;

import io.micronaut.context.annotation.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("devices")
public class DevicesConfiguration {
    private List<String> powerstreams;
    private List<String> batteries;

    public List<String> getPowerstreams() {
        return powerstreams;
    }

    public void setPowerstreams(List<String> powerstreams) {
        this.powerstreams = powerstreams;
    }

    public List<String> getBatteries() {
        return batteries;
    }

    public void setBatteries(List<String> batteries) {
        this.batteries = batteries;
    }
}