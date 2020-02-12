package com.sokolmeteo.satellitemessageservice.config;

import com.sokolmeteo.satellitemessageservice.tcp.TCPClientService;
import com.sokolmeteo.satellitemessageservice.tcp.TCPServer;
import com.sokolmeteo.satellitemessageservice.repo.IridiumMessageRepository;
import com.sokolmeteo.satellitemessageservice.tcp.TCPClient;
import com.sokolmeteo.satellitemessageservice.tcp.impl.SokolClient;
import com.sokolmeteo.satellitemessageservice.tcp.impl.TCPServerImpl;
import com.sokolmeteo.satellitemessageservice.tcp.TCPServerMessageProcessor;
import com.sokolmeteo.satellitemessageservice.tcp.impl.IridiumMessageProcessorImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScans({
        @ComponentScan(basePackageClasses = TCPServer.class),
        @ComponentScan(basePackageClasses = TCPClient.class)
})
@EnableConfigurationProperties(ApplicationProperties.class)
public class ApplicationConfig {
    private ApplicationProperties properties;
    private IridiumMessageRepository repository;

    @Autowired
    public void setProperties(ApplicationProperties properties) {
        this.properties = properties;
    }

    @Autowired
    public void setRepository(IridiumMessageRepository repository) {
        this.repository = repository;
    }

    @Bean
    public TCPServerMessageProcessor getMessageProcessor() {
        return new IridiumMessageProcessorImpl(repository);
    }

    @Bean
    public TCPServer getTCPServer() {
        return new TCPServerImpl(
                properties.getServerPort(),
                properties.getServerSocketTimeout(),
                properties.getServerMaxMessageSize(),
                properties.getServerIncomingMessageInterval(),
                getMessageProcessor()
        );
    }

    @Bean
    public TCPClient getTCPClient() {
        return new SokolClient(
                properties.getClientAddresseeHost(),
                properties.getClientAddresseePort(),
                properties.getClientCredential(),
                properties.getClientMaxMessageSize()
        );
    }

    @Bean
    public TCPClientService getTCPClientService() {
        return new TCPClientService(getTCPClient(), repository);
    }
}
