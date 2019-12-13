package com.sokolmeteo.satellitemessageservice.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "server")
@Data
public class ServerProperties {
    private int port;
    private final int socketTimeout = 300000;
    private final int maxMessageSize = 512 * 1024;
    private final int incomingMessageInterval = 1000;
}
