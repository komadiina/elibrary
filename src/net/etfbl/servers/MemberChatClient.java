package net.etfbl.servers;

import java.io.*;
import java.util.function.Consumer;
import javax.net.ssl.*;

import net.etfbl.model.User;

public class MemberChatClient {

    private final User user;
    private final SSLContext sslContext;
    private final Consumer<String> messageHandler;
    private SSLSocket socket;
    private PrintWriter out;
    private BufferedReader in;

    public MemberChatClient(User user, SSLContext sslContext, Consumer<String> messageHandler) {
        this.user = user;
        this.sslContext = sslContext;
        this.messageHandler = messageHandler;
    }

    public void start() throws IOException {
        SSLSocketFactory socketFactory = sslContext.getSocketFactory();
        socket = (SSLSocket) socketFactory.createSocket("localhost", 9999);

        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Sending the username to the server
        out.println(user.getUsername() + " joined the chatroom.");

        // Listening for messages from the server
        new Thread(this::listenForMessages).start();
    }

    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                messageHandler.accept(message);
            }
        } catch (IOException e) {
            System.err.println("Error reading messages: " + e.getMessage());
            close();
        }
    }

    public void sendMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        out.println(message);
    }

    public void close() {
        try {
            if (socket != null) {
                socket.close();
            }
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing chat client: " + e.getMessage());
        }
    }
}
