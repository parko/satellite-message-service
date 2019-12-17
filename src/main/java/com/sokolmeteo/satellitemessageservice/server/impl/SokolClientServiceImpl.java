package com.sokolmeteo.satellitemessageservice.server.impl;

import com.sokolmeteo.satellitemessageservice.server.TCPClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.Socket;

@Slf4j
@Service
public class SokolClientServiceImpl implements TCPClientService {

    @Override
    public boolean sendMessage(String loginMessage, String blackMessage, int port) {
        try {
            Socket socket = new Socket("localhost", port);
            socket.getOutputStream().write(loginMessage.getBytes());
            byte[] response = new byte[64 * 1024];
            int r = socket.getInputStream().read(response);
            log.info(String.format("Logining to Sokol[%d]: %s",
                    port,
                    new String(response, 0, r)));

            socket.getOutputStream().write(blackMessage.getBytes());
            r = socket.getInputStream().read(response);
            log.info("Sending to Sokol: " + new String(response, 0, r));
        } catch (IOException e) {
            log.info("Exception on sending to Sokol");
            return false;
        }
        return true;
    }
}
