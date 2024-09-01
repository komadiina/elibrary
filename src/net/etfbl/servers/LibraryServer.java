package net.etfbl.servers;

import java.io.*;
import java.net.*;
import java.util.*;

import net.etfbl.config.Configuration;
import net.etfbl.config.IOLogger;

public class LibraryServer {
	private static final IOLogger logger = new IOLogger(LibraryServer.class.getName());
	
	public static volatile List<String> suppliers = new ArrayList<>();
	public static volatile HashMap<String, List<String>> supplierOffer = new HashMap<String, List<String>>();
	
	private static final int PORT = Integer.parseInt(Configuration.projectProperties.getProperty("libraryServerPort"));
	private static final String HOST = "localhost";
	
	public static volatile boolean running = true;
	
	public static volatile Double recentVAT = 0.0;
	public static final Object bell = new Object();
	
	public static void main(String[] args) throws Exception {
		ServerSocket ss = null;
		try {
			ss = new ServerSocket(PORT);
			logger.info(String.format("Initialized ServerSocket at %s:%d.", HOST, PORT));
			
			while (true) {
				Socket clientSocket = ss.accept();
				LibraryServerThread serverThread = new LibraryServerThread(clientSocket);
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
		
		logger.info("Closed LibraryServer @ " + HOST + ":" + PORT);
	}
}
