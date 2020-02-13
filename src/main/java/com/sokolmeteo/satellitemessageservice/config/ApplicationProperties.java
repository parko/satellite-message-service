package com.sokolmeteo.satellitemessageservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "satellite")
public class ApplicationProperties {
    private int serverPort;
    private int serverSocketTimeout;
    private int serverMaxMessageSize;
    private int serverIncomingMessageInterval;
    private String clientAddresseeHost;
    private int clientAddresseePort;
    private String clientCredential;
    private int clientMaxMessageSize;
}
