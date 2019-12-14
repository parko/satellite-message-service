package com.sokolmeteo.satellitemessageservice.server;

import com.sokolmeteo.satellitemessageservice.dto.Client;

import java.io.UnsupportedEncodingException;

public interface TCPServerMessageProcessor {
    void clientConnected(Client client);

    void ClientDisconnected(Client client);

    void processMessage(Client client, byte[] message) throws UnsupportedEncodingException;
}
