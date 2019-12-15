package com.sokolmeteo.satellitemessageservice.service;

import com.sokolmeteo.satellitemessageservice.dto.IridiumMessage;
import com.sokolmeteo.satellitemessageservice.server.TCPClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {
    private final TCPClientService tcpClientService;
    private final MessageService messageService;

    public void exportMessages(List<IridiumMessage> messages) {
        Map<String, List<IridiumMessage>> grouppedMessages = groupByImei(messages);
        Set<String> imeis = grouppedMessages.keySet();
        for (String imei : imeis) {
            tcpClientService.sendMessage(messageService.generateLoginMessage(imei));
            tcpClientService.sendMessage(messageService.iridiumToBlackMessage(grouppedMessages.get(imei)));
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
