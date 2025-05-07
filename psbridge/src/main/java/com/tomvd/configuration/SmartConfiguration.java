package com.tomvd.configuration;

import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("smart")
public class SmartConfiguration {
    boolean enabled;
    String meterTopic;
    String enabledTopic;
    String chargerTopic;
    String socTopic;
    Integer maxPower;
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getMeterTopic() { return meterTopic; }
    public void setMeterTopic(String meterTopic) { this.meterTopic = meterTopic; }
    public String getEnabledTopic() { return enabledTopic; }
    public void setEnabledTopic(String enabledTopic) { this.enabledTopic = enabledTopic; }
    public String getChargerTopic() { return chargerTopic; }
    public void setChargerTopic(String chargerTopic) { this.chargerTopic = chargerTopic; }
    public String getSocTopic() { return socTopic; }
    public void setSocTopic(String socTopic) { this.socTopic = socTopic; }
    public Integer getMaxPower() { return maxPower; }
    public void setMaxPower(Integer maxPower) { this.maxPower = maxPower; }
}
