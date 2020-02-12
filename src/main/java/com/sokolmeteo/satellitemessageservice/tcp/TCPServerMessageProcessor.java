package com.sokolmeteo.satellitemessageservice.tcp;

import java.io.UnsupportedEncodingException;

public interface TCPServerMessageProcessor {
    void processMessage(byte[] message) throws UnsupportedEncodingException;
}
