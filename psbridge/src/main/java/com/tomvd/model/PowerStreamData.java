package com.tomvd.model;

public record PowerStreamData(double avgVoltage, int currentPower, String upstreamTopic,String commandTopic) {
    public PowerStreamData withAvgVoltage(double newAvgVoltage) {
        return new PowerStreamData(newAvgVoltage, currentPower, upstreamTopic, commandTopic);
    }

    public PowerStreamData withCurrentPower(int newCurrentPower) {
        return new PowerStreamData(avgVoltage, newCurrentPower, upstreamTopic, commandTopic);
    }
}
