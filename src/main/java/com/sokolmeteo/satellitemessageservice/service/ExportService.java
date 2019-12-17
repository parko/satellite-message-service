package com.sokolmeteo.satellitemessageservice.service;

import com.sokolmeteo.satellitemessageservice.config.properties.AppilcationProperties;
import com.sokolmeteo.satellitemessageservice.dto.IridiumMessage;
import com.sokolmeteo.satellitemessageservice.repo.IridiumMessageRepository;
import com.sokolmeteo.satellitemessageservice.server.TCPClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ExportService {
    private final AppilcationProperties appilcationProperties;
    private final TCPClientService tcpClientService;
    private final MessageService messageService;
    private final IridiumMessageRepository iridiumMessageRepository;

    public void exportMessages() {
        List<IridiumMessage> iridiumMessages = iridiumMessageRepository.findByErrorCounterAndSent(0, false);
        if (iridiumMessages.size() > 0) {
            Map<String, List<IridiumMessage>> grouppedMessages = groupByImei(iridiumMessages);

            for (String imei : grouppedMessages.keySet()) {
                List<IridiumMessage> messages = grouppedMessages.get(imei);
                boolean response = tcpClientService.sendMessage(
                        messageService.generateLoginMessage(imei),
                        messageService.iridiumToBlackMessage(messages),
                        appilcationProperties.getSokolPort());
                if (response) {
                    for (IridiumMessage message : messages) {
                        message.setSent(true);
                    }
                    iridiumMessageRepository.saveAll(messages);
                }
            }
        }
    }

    private Map<String, List<IridiumMessage>> groupByImei(List<IridiumMessage> messages) {
        Map<String, List<IridiumMessage>> grouppedMessages = new HashMap<>();
        for (IridiumMessage message : messages) {
            if (grouppedMessages.containsKey(message.getImei())) {
                grouppedMessages.get(message.getImei()).add(message);
            } else {
                List<IridiumMessage> messages1 = new ArrayList<>();
                messages1.add(message);
                grouppedMessages.put(message.getImei(), messages1);
            }
        }
        return grouppedMessages;
    }
}
