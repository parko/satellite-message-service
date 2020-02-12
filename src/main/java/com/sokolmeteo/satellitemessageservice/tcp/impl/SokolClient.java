package com.sokolmeteo.satellitemessageservice.tcp.impl;

import com.sokolmeteo.satellitemessageservice.tcp.TCPClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SokolClient implements TCPClient {
    private final String sokolHost;
    private final int sokolPort;
    private final String credential;
    private final int sokolMaxMessageSize;

    public SokolClient(String sokolHost, int sokolPort, String credential, int sokolMaxMessageSize) {
        this.sokolHost = sokolHost;
        this.sokolPort = sokolPort;
        this.credential = credential;
        this.sokolMaxMessageSize = sokolMaxMessageSize;
    }

    @Override
    public boolean sendMessage(String imei, String blackMessage) {
        try (Socket socket = new Socket(sokolHost, sokolPort);
             OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream()
        ) {
            out.write(getLoginMessage(imei).getBytes());
            byte[] response = new byte[sokolMaxMessageSize];
            int r = in.read(response);
            System.out.println("Logs in to Sokol: "+ new String(response, 0, r));

            out.write(blackMessage.getBytes());
            r = socket.getInputStream().read(response);
            System.out.println("Sending to Sokol: " + new String(response, 0, r));
        } catch (IOException e) {
            System.out.println("Exception on sending to Sokol");
            return false;
        }
        return true;
    }

    private String getLoginMessage(String imei) {
        return String.format("#L#%s;%s\r\n", imei, credential);
    }
}
