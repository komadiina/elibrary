package net.etfbl.servers;

import java.io.*;
import java.net.*;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

import net.etfbl.config.IOLogger;

public class LibraryServerThread extends Thread {
	private static final IOLogger logger = new IOLogger(LibraryServerThread.class.getName());
	
	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;
	
	public LibraryServerThread(Socket socket) {
		this.socket = socket;
		
		try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
		} catch (IOException exception) {
			logger.severe(String.format("Could not instantiate socket communication with %s:%d",
					socket.getInetAddress().toString(),
					socket.getPort()
					));
		}
	}
	
	@Override
	public void run() {
		boolean running = true;
		String request = "";
		String response = "";
		
		while (running) {
			try {
				request = in.readLine();
                
                if (request == null) {
                    running = false;
                } else if (request.startsWith("HELLO")) {
                    handleHelloMessage(request);
                } else if (request.startsWith("UPDATE_LIST")) {
                	// handleSupplierListUpdate(request);
                } else if (request.startsWith("GET_SUPPLIERS")) {
                	response = serializeSupplierOffers();
                	out.println(response);
                } else if (request.startsWith("VAT")) {
                	LibraryServer.recentVAT = Double.parseDouble(request.split(" ")[1]);
                	
                	synchronized (LibraryServer.bell) {
						LibraryServer.bell.notify();
					}
                }
			} catch (IOException exception) {
				logger.warning("IOException occurred, reason: " + exception.getMessage());
				logger.info("Last request: " + request);
				logger.info("Last response: " + response);
				exception.printStackTrace();
			} finally {
				running = false;
				
	            try {
	                in.close();
	                out.close();
	                socket.close();
	            } catch (IOException e) {
	                logger.severe("Failed to close resources: " + e.getMessage());
	            }
	        }
		}
	}
	
	private void handleHelloMessage(String message) {
        String[] parts = message.split(" ");
        if (parts.length < 2) {
            logger.warning("Invalid HELLO message format.");
            return;
        }
        
        String supplierId = parts[1];
        List<String> bookIds = new ArrayList<>();
        for (int i = 2; i < parts.length; i++) {
            bookIds.add(parts[i]);
        }
        
        LibraryServer.suppliers.add(supplierId);
        LibraryServer.supplierOffer.put(supplierId, bookIds);
        
        logger.info(String.format(
        		"Accepted connection from \"%s\", available books: %s", 
        		supplierId, 
        		String.join(", ", bookIds)));
    }
	
	private String serializeSupplierOffers() {
	    JSONObject jsonResponse = new JSONObject();
	    
	    for (Map.Entry<String, List<String>> entry : LibraryServer.supplierOffer.entrySet()) {
	        String supplier = entry.getKey();
	        List<String> books = entry.getValue();
	        JSONArray booksArray = new JSONArray(books);
	        jsonResponse.put(supplier, booksArray);
	    }
	    
	    return jsonResponse.toString();
	}
}
