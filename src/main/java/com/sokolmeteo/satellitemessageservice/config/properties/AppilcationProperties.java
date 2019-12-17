package com.sokolmeteo.satellitemessageservice.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "server")
@Data
public class AppilcationProperties {
    private int port;
    private final int socketTimeout = 300000;
    private final int maxMessageSize = 512 * 1024;
    private final int incomingMessageInterval = 1000;
    private final String sokolPassword = "2211";
    private final int sokolPort = 8001;
}
