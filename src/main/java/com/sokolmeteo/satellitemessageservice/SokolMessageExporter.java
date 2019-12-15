package com.sokolmeteo.satellitemessageservice;

import com.sokolmeteo.satellitemessageservice.dto.IridiumMessage;
import com.sokolmeteo.satellitemessageservice.repo.IridiumMessageRepository;
import com.sokolmeteo.satellitemessageservice.service.ExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class SokolMessageExporter {
    private final IridiumMessageRepository iridiumMessageRepository;
    private final ExportService exportService;

    @Scheduled(fixedDelay = 60000)
    public void init() {
        List<IridiumMessage> messages = iridiumMessageRepository.findByErrorCounterAndSent(0, false);
        if (messages.size() > 0) {
            exportService.exportMessages(messages);
        }
    }
}
