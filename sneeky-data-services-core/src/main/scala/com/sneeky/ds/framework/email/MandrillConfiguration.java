package com.sneeky.ds.framework.email;

/**
 * Created by aparrish on 2/22/14.
 */
public class MandrillConfiguration {

    public String apiKey;
    public String smtpPort;
    public String smtpHost;
    public String username;

    public MandrillConfiguration(String apiKey, String smtpPort, String smtpHost, String username) {
        this.apiKey = apiKey;
        this.smtpPort = smtpPort;
        this.smtpHost = smtpHost;
        this.username = username;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(String smtpPort) {
        this.smtpPort = smtpPort;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
