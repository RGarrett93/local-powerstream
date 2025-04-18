package com.tomvd.configuration;

import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("mqtt")
public class MQTTConfiguration {
    private EcoflowConfig ecoflow = new EcoflowConfig();
    private HomeAssistantConfig homeAssistant = new HomeAssistantConfig();
    private boolean enableDiscovery;

    @ConfigurationProperties("ecoflow")
    public static class EcoflowConfig {
        private String url;
        private String username;
        private String password;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    @ConfigurationProperties("home-assistant")
    public static class HomeAssistantConfig {
        private String url;
        private String username;
        private String password;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public EcoflowConfig getEcoflow() { return ecoflow; }
    public void setEcoflow(EcoflowConfig ecoflow) { this.ecoflow = ecoflow; }
    public HomeAssistantConfig getHomeAssistant() { return homeAssistant; }
    public void setHomeAssistant(HomeAssistantConfig homeAssistant) { this.homeAssistant = homeAssistant; }
    public boolean isEnableDiscovery() { return enableDiscovery; }
    public void setEnableDiscovery(boolean enableDiscovery) { this.enableDiscovery = enableDiscovery; }
}
