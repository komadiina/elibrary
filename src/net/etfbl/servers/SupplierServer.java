package net.etfbl.servers;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import net.etfbl.config.Configuration;
import net.etfbl.config.IOLogger;

public class SupplierServer {
	private static final IOLogger logger = new IOLogger(SupplierServer.class.getName());
	private static final int PORT = Integer.parseInt(Configuration.projectProperties.getProperty("supplierServerPort"));
	private static final String HOST = "localhost";
	
	public static volatile boolean running = true;
	
	public static void main(String[] args) throws Exception {
		ServerSocket ss = null;
		try {
			ss = new ServerSocket(PORT);
			logger.info(String.format("Initialized ServerSocket at %s:%d.", HOST, PORT));
			
			while (true) {
				Socket clientSocket = ss.accept();
				SupplierServerThread serverThread = new SupplierServerThread(clientSocket);
				serverThread.start();
				
				logger.info("Instantiated and started a new server sub-thread.");
			}
		} catch (Exception exc) {
			logger.warning("Unknown exception occurred: " + exc.getMessage());
		} finally {
			try {
				if (ss != null)
					ss.close();
			} catch (IOException exception) {
				logger.severe("ServerSocket could not be cloesd.");
			}
		}
		
		logger.info("Closed SupplierServer @ " + HOST + ":" + PORT);
	}
}
