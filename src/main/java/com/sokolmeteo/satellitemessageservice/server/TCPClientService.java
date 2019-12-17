package com.sokolmeteo.satellitemessageservice.server;

import java.io.IOException;

public interface TCPClientService {
    boolean sendMessage(String loginMessage, String blackMessage, int port);
}
