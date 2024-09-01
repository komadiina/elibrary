package net.etfbl.servers;

import java.io.*;
import java.net.*;
import java.security.KeyStore;
import javax.net.ssl.*;

import net.etfbl.middleware.SSLContextUtil;

import java.util.ArrayList;
import java.util.List;

public class MemberChatServer {
    private static final int PORT = 9999;
    private List<ClientHandler> clients = new ArrayList<>();
    private SSLServerSocket serverSocket;

    public MemberChatServer() throws Exception {
        SSLContext sslContext = SSLContextUtil.getSSLContext();
        SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
        serverSocket = (SSLServerSocket) ssf.createServerSocket(PORT);
    }

    public void start() {
        System.out.println("Chat Server started on port " + PORT);
        while (true) {
            try {
                SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, this);
                clients.add(handler);
                new Thread(handler).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        MemberChatServer server = new MemberChatServer();
        server.start();
    }
}

class ClientHandler implements Runnable {
    private SSLSocket socket;
    private MemberChatServer server;
    private PrintWriter out;
    private BufferedReader in;

    public ClientHandler(SSLSocket socket, MemberChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String message;
            while ((message = in.readLine()) != null) {
                server.broadcastMessage(message, this);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}