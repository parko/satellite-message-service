package com.sokolmeteo.satellitemessageservice.server.impl;

import com.sokolmeteo.satellitemessageservice.dto.Client;
import com.sokolmeteo.satellitemessageservice.server.TCPServerMessageProcessor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IridiumMessageProcessorImpl implements TCPServerMessageProcessor {
    @Override
    public void clientConnected(Client client) {
        log.info(String.format("[%s] - connection opened", client.getId()));
    }

    @Override
    public void ClientDisconnected(Client client) {
        log.info(String.format("[%s] - connection closed", client.getId()));
    }

    @Override
    public void processMessage(Client client, byte[] message) {

    }
}
