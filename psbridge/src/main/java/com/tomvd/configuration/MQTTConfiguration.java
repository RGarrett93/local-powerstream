package com.tomvd.configuration;

import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("mqtt.client")
public class MQTTConfiguration {
    private boolean enableDiscovery;
    private String serverUri;
    private String userName;
    private String password;
    public String getServerUri() { return serverUri; }
    public void setServerUri(String url) { this.serverUri = url; }
    public String getUserName() { return userName; }
    public void setUserName(String username) { this.userName = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public boolean isEnableDiscovery() { return enableDiscovery; }
    public void setEnableDiscovery(boolean enableDiscovery) { this.enableDiscovery = enableDiscovery; }
}
