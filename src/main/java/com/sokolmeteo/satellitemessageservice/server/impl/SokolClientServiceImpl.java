package com.sokolmeteo.satellitemessageservice.server.impl;

import com.sokolmeteo.satellitemessageservice.server.TCPClientService;
import org.springframework.stereotype.Service;

@Service
public class SokolClientServiceImpl implements TCPClientService {
    @Override
    public void sendMessage(String message) {
        System.out.println(message);
    }
}
