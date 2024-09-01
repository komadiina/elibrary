package net.etfbl.servers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import net.etfbl.config.IOLogger;
import net.etfbl.middleware.BooksMiddleware;
import net.etfbl.model.Book;

public class SupplierServerThread extends Thread {
	private static final IOLogger logger = new IOLogger(SupplierServerThread.class.getName());
	
	private static BooksMiddleware backend = BooksMiddleware.getInstance();
	
	private final Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	
	public SupplierServerThread(Socket socket) {
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
	
	
	/*
	@Override
	public void run() {
		boolean serving = true;
		String request = "";
		String response = "";
		
		while (serving) {
			try {
				request = in.readLine();
				
				if (request.equals("END_SESSION")) {
					logger.info(String.format("[%s:d] Received session shutdown signal.", 
							socket.getInetAddress().toString(), socket.getPort()));
					serving = false;
				} else if (request.startsWith("ADD_BOOK")) {
					// ADD_BOOK {String id}
					String id = request.split(" ")[1];
					String result = backend.addFromPG(id);
					
					if (result != null) 
						out.println("Successfully added book: " + result);
					else out.println("Failed to add book.");
				} else if (request.equals("GET_BOOKS")) {
					List<Book> availableBooks = backend.getAll();
					JSONArray jsonArray = new JSONArray();
					for (Book book : availableBooks)
						jsonArray.put(new JSONObject(book.toMap()));
					
					logger.info("Writing books to output stream...");
					logger.info(jsonArray.toString());
					
					out.println(jsonArray.toString());
				}
			} catch (IOException exception) {
				logger.warning(String.format("IOException occurred (%s:%d): %s",
						socket.getInetAddress().toString(),
						socket.getPort(),
						exception.getMessage()));
				exception.printStackTrace();
				serving = false;
			}
			
			
		    try {
		        in.close();
		        out.close();
		        socket.close();
		    } catch (IOException e) {
		        logger.severe("Failed to close resources: " + e.getMessage());
		    }
		}
	}*/
	
	@Override
	public void run() {
	    boolean serving = true;
	    String request;

	    try {
	        while (serving) {
	            request = in.readLine();

	            if (request == null) {
	                serving = false;
	            } else if (request.equals("END_SESSION")) {
	                logger.info(String.format("[%s:%d] Received session shutdown signal.",
	                        socket.getInetAddress().toString(), socket.getPort()));
	                serving = false;
	            } else if (request.startsWith("ADD_BOOK")) {
	                String id = request.split(" ")[1];
	                String result = backend.addFromPG(id);

	                if (result != null) 
	                    out.println("Successfully added book: " + result);
	                else 
	                    out.println("Failed to add book.");
	            } else if (request.equals("GET_BOOKS")) {
	                List<Book> availableBooks = backend.getAll();
	                JSONArray jsonArray = new JSONArray();
	                for (Book book : availableBooks) {
	                    jsonArray.put(new JSONObject(book.toMap()));
	                }

	                logger.info("Writing books to output stream...");
	                logger.info(jsonArray.toString());

	                out.println(jsonArray.toString());
	            }
	        }
	    } catch (IOException exception) {
	        logger.warning(String.format("IOException occurred (%s:%d): %s",
	                socket.getInetAddress().toString(),
	                socket.getPort(),
	                exception.getMessage()));
	        exception.printStackTrace();
	    } finally {
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
