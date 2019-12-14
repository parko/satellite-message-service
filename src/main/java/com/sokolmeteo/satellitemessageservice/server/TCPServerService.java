package com.sokolmeteo.satellitemessageservice.server;

import com.sokolmeteo.satellitemessageservice.config.properties.ServerProperties;
import com.sokolmeteo.satellitemessageservice.dto.Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

@Slf4j
@Service
@EnableConfigurationProperties(ServerProperties.class)
@RequiredArgsConstructor
public class TCPServerService extends Thread {
    private final ServerProperties serverProperties;
    private final TCPServerMessageProcessor messageProcessor;

    private boolean running = false;
    private ServerSocket serverSocket = null;
    private final LinkedList<Connection> connections = new LinkedList<>();


    @PostConstruct
    public void startServer() {
        if (!running) {
            super.start();
        }
    }

    @PreDestroy
    public void stopServer() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                log.warn("Error on closing server socket to port " + serverProperties.getPort() + ": " + e.toString());
            }
        }
    }

    public void run() {
        running = true;

        try {
            bind(serverProperties.getPort());
            listen();
        } catch (IOException e) {
            log.error("Error on starting server on port " + serverProperties.getPort() + " caused by " + e.toString() + ";");
        }
    }

    private void bind(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    private void listen() throws IOException {
        while (running) {
            Socket socket = serverSocket.accept();
            socket.setSoLinger(true, 1);
            socket.setSoTimeout(serverProperties.getSocketTimeout());

            Connection connection = new Connection(socket);
            connection.init();

            synchronized (connections) {
                connections.addLast(connection);
                updateConnectionStatus();
            }

            connection.start();
        }
    }

    private void onCloseConnection(Connection connection) {
        synchronized (connections) {
            connections.remove(connection);
            updateConnectionStatus();
        }
    }

    public boolean isStarted() {
        return running;
    }

    private void updateConnectionStatus() {
        connections.notifyAll();
    }

    private class Connection extends Thread {
        private Socket connectionSocket;
        private DataInputStream inputStream;
        private DataOutputStream outputStream;
        private Client client = new Client();
        private boolean active = true;

        Connection(Socket socket) {
            this.connectionSocket = socket;
        }

        private void init() throws IOException {
            outputStream = new DataOutputStream(connectionSocket.getOutputStream());
            inputStream = new DataInputStream(connectionSocket.getInputStream());

            if (client != null) {
                messageProcessor.clientConnected(client);
            }
        }

        private void close() throws IOException {
            active = false;
            if (client != null) {
                messageProcessor.ClientDisconnected(client);
            }
            connectionSocket.close();
            onCloseConnection(this);
        }

        public void run() {
            while (active && !Thread.currentThread().isInterrupted()) {
                try {
                    byte[] stream = TCPServerUtils.readBytes(inputStream, serverProperties.getMaxMessageSize());
                    if (stream == null) break;
                    if (stream.length > serverProperties.getMaxMessageSize()) {
                        log.warn("Message is too big");
                        close();
                        return;
                    }
                    messageProcessor.processMessage(client, stream);

                    try {
                        sleep(serverProperties.getIncomingMessageInterval());
                    } catch (InterruptedException e) {
                        log.error("Thread interruption exception");
                    }
                    close();

                } catch (IOException e) {
                    log.warn("Error on starting connection caused by: " + e.toString());
                }

            }
        }
    }
}
