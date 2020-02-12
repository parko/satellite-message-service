package com.sokolmeteo.satellitemessageservice.tcp.impl;

import com.sokolmeteo.satellitemessageservice.tcp.TCPServer;
import com.sokolmeteo.satellitemessageservice.tcp.TCPServerMessageProcessor;
import com.sokolmeteo.satellitemessageservice.tcp.TCPServerUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class TCPServerImpl extends Thread implements TCPServer {
    private final TCPServerMessageProcessor messageProcessor;
    private final int port;
    private final int socketTimeout;
    private final int maxMessageSize;
    private final int incomingMessageInterval;

    public TCPServerImpl(int port, int socketTimeout, int maxMessageSize, int incomingMessageInterval, TCPServerMessageProcessor messageProcessor) {
        this.port = port;
        this.socketTimeout = socketTimeout;
        this.maxMessageSize = maxMessageSize;
        this.incomingMessageInterval = incomingMessageInterval;
        this.messageProcessor = messageProcessor;
    }

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
        disconnectConnections();
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (Exception ignored) { }
        }
    }

    public void disconnectConnections() {
        synchronized (connections) {
            for (Connection connection : connections) {
                connection.close();
            }
        }
    }

    public void run() {
        running = true;
        System.out.println("Binding to port: " + port);
        bind(port);
        listen();
    }

    private void bind(int port) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println(String.format("Error binding to port %d %s;", port, e.toString()));
        }
    }

    private void listen() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                socket.setSoLinger(true, 1);
                socket.setSoTimeout(socketTimeout);

                Connection connection = new Connection(socket);
                connection.init();

                synchronized (connections) {
                    connections.addLast(connection);
                    updateConnectionStatus();
                }

                connection.start();
            } catch (IOException e) {
                System.out.println("Exception when listening." + e);
            }
        }
    }

    private void onCloseConnection(Connection connection) {
        synchronized (connections) {
            connections.remove(connection);
            updateConnectionStatus();
        }

        System.out.println("Connection closed");
    }

    public boolean isStarted() {
        return running;
    }

    private void updateConnectionStatus() {
        connections.notifyAll();
    }

    private class Connection extends Thread {
        private boolean active = true;
        private Socket connectionSocket;
        private DataInputStream inputStream;
        private DataOutputStream outputStream;

        Connection(Socket socket) {
            this.connectionSocket = socket;
        }

        private void init() throws IOException {
            outputStream = new DataOutputStream(connectionSocket.getOutputStream());
            inputStream = new DataInputStream(connectionSocket.getInputStream());
            System.out.println("There is connection");
        }

        private void close() {
            active = false;
            try {
                connectionSocket.close();
                inputStream.close();
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                System.out.println("Exception when closing a socket" + e);
            }
            onCloseConnection(this);
        }

        public void run() {
            try {
                while (active && !Thread.currentThread().isInterrupted()) {
                    byte[] stream = TCPServerUtils.readBytes(inputStream, maxMessageSize);
                    if (stream == null) break;

                    messageProcessor.processMessage(stream);

                    try {
                        sleep(incomingMessageInterval);
                    } catch (InterruptedException e) {
                        System.out.println("Thread interruption exception " + e);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error on starting connection caused by: " + e.toString());
            }
            close();
        }
    }
}
