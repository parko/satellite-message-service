package com.sokolmeteo.satellitemessageservice.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
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
