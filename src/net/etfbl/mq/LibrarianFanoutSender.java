package net.etfbl.mq;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.*;

public class LibrarianFanoutSender {
	private static final String EXCHANGE_NAME = "PROPOSALS";
	
	public void queueMessage(String message) throws IOException, TimeoutException {
		System.out.println(message);
		
		Connection connection = ConnectionFactoryUtil.createConnection();
		Channel channel = connection.createChannel();
		channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
		
		channel.basicPublish(EXCHANGE_NAME, "", null, message.getBytes(StandardCharsets.UTF_8));
		
		channel.close();
		connection.close();
	}
}
