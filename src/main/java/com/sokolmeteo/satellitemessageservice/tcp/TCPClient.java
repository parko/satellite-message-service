package com.sokolmeteo.satellitemessageservice.tcp;

public interface TCPClient {
    boolean sendMessage(String imei, String blackMessage);
}
