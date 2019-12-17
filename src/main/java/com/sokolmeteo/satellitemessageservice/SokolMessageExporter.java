package com.sokolmeteo.satellitemessageservice;

import com.sokolmeteo.satellitemessageservice.service.ExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class SokolMessageExporter {
    private final ExportService exportService;

    @Scheduled(fixedDelay = 60000)
    public void init() {
        exportService.exportMessages();
    }
}
