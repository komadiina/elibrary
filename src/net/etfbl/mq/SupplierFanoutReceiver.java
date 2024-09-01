package net.etfbl.mq;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class SupplierFanoutReceiver {
    private static final String EXCHANGE_NAME = "PROPOSALS";
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

    public SupplierFanoutReceiver() throws IOException, TimeoutException {
        Connection connection = ConnectionFactoryUtil.createConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);

        System.out.println(1);
        
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "");

        System.out.println(2);
        
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                    byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                try {
                    messageQueue.put(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Failed to put message into queue", e);
                }
            }
        };

        System.out.println(3);
        channel.basicConsume(queueName, true, consumer);
        System.out.println(4);
        
        channel.close();
        connection.close();
    }

    public String dequeueMessage() throws InterruptedException {
        return messageQueue.take();
    }
}