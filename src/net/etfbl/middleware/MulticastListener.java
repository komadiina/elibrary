package net.etfbl.middleware;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.function.Consumer;
import java.util.function.Function;

import net.etfbl.config.IOLogger;

public class MulticastListener extends Thread {
    private static final IOLogger logger = new IOLogger(MulticastListener.class.getName());
    private final MulticastSocket multicastSocket;
    private final InetAddress group;
    private volatile boolean running = true;
    private Function<String, String> customLogic = null;

    public MulticastListener(MulticastSocket multicastSocket, InetAddress group) {
        this.multicastSocket = multicastSocket;
        this.group = group;
    }
    
    public MulticastListener(MulticastSocket multicastSocket, InetAddress group, Function<String, String> messageConsumer) {
        this.multicastSocket = multicastSocket;
        this.group = group;
        this.customLogic = messageConsumer;
    }

    @SuppressWarnings("deprecation")
	@Override
    public void run() {
        try {
            multicastSocket.joinGroup(group);
            logger.info("Joined multicast group: " + group.getHostAddress());

            while (running) {
                byte[] buffer = new byte[2048];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                multicastSocket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                logger.info("Received multicast message: " + message);
                
                String response = "";
                
                if (customLogic == null) processMessage(message);
                else response = customLogic.apply(response);
                
                buffer = response.getBytes();
                packet = new DatagramPacket(buffer, buffer.length, group, packet.getPort());
                multicastSocket.send(packet);
            }
        } catch (IOException e) {
            logger.severe("Failed to receive multicast message: " + e.getMessage());
        } finally {
            try {
                multicastSocket.leaveGroup(group);
                multicastSocket.close();
                logger.info("Left multicast group: " + group.getHostAddress());
            } catch (IOException e) {
                logger.severe("Failed to leave multicast group: " + e.getMessage());
            }
        }
    }

    public void shutdown() {
        running = false;
        multicastSocket.close();
    }

    private void processMessage(String message) {
        // TODO
        logger.info("Processing message: " + message);
    }
}