package com.sokolmeteo.satellitemessageservice.server.impl;

import com.sokolmeteo.satellitemessageservice.dto.Client;
import com.sokolmeteo.satellitemessageservice.dto.IridiumMessage;
import com.sokolmeteo.satellitemessageservice.repo.IridiumMessageRepository;
import com.sokolmeteo.satellitemessageservice.server.TCPServerMessageProcessor;
import com.sokolmeteo.satellitemessageservice.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IridiumMessageProcessorImpl implements TCPServerMessageProcessor {
    private final IridiumMessageRepository iridiumMessageRepository;
    private final MessageService messageService;

    @Override
    public void clientConnected(Client client) {
        log.info(String.format("[%s] - connection opened", client.getId()));
    }

    @Override
    public void ClientDisconnected(Client client) {
        log.info(String.format("[%s] - connection closed", client.getId()));
    }

    @Override
    public void processMessage(Client client, byte[] bytes) {
        IridiumMessage iridiumMessage = messageService.byteArrayToIridiumMessage(bytes);
        iridiumMessageRepository.save(iridiumMessage);
        log.info(String.format("[%d] - message received", iridiumMessage.getId()));
    }
}
