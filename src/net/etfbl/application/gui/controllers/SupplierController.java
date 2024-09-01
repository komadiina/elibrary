package net.etfbl.application.gui.controllers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import net.etfbl.config.Configuration;
import net.etfbl.config.IOLogger;
import net.etfbl.middleware.Utility;
import net.etfbl.model.Book;
import net.etfbl.model.Receipt;
import net.etfbl.model.User;
import net.etfbl.model.rmi.AccountingInterface;
import net.etfbl.mq.SupplierFanoutReceiver;

public class SupplierController implements Initializable {
	private static final String SUPPLIER_SERVER_PORT = Configuration.projectProperties.getProperty("supplierServerPort");
	private static final String SUPPLIER_SERVER_HOST = "localhost";
	private static final String LIBRARY_SERVER_HOST = "localhost";
	private static final String LIBRARY_SERVER_PORT = Configuration.projectProperties.getProperty("libraryServerPort");
	private static final IOLogger logger = new IOLogger(SupplierController.class.getName());
	
	private User user = Configuration.currentUser;
	
	private BufferedReader in;
	private PrintWriter out;
	private Socket socket;
	
    private SupplierFanoutReceiver fanoutReceiver;
    private ScheduledExecutorService scheduler;
    AccountingInterface accountingService = null;
    
	public TextField titleFilter;
	public TextField supplyGutenbergID;
	public ListView<String> bookListView;
	private ListView<String> bookListViewBackup = new ListView<>();
	private ObservableSet<String> observableBookSet;
	public ListView<String> supplyRequestView;
	
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		bookListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		observableBookSet = FXCollections.observableSet();
		
		observableBookSet.addListener((SetChangeListener.Change<? extends String> c) -> {
			if (c.wasAdded())
				bookListView.getItems().add(c.getElementAdded());
			if (c.wasRemoved())
				bookListView.getItems().remove(c.getElementRemoved());
		});
		
		try {
			socket = new Socket(InetAddress.getByName(SUPPLIER_SERVER_HOST), Integer.parseInt(SUPPLIER_SERVER_PORT));
			out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			fanoutReceiver = new SupplierFanoutReceiver();
            startMessageListener();
		} catch (TimeoutException exception) {
			logger.severe("Failed to establish connection with the message queue: " + exception.getMessage());
		} catch (IOException exception) {
			logger.severe(String.format("Failed to open socket %s:%s, reason: %s", SUPPLIER_SERVER_HOST, SUPPLIER_SERVER_PORT, exception.getMessage()));
		}
		
		List<Book> books = fetchBooks(); 
		observableBookSet.addAll(books.stream().map(x -> x.prettyString()).toList());
		bookListView.getItems().setAll(books.stream().map(book -> book.prettyString()).toList());
		bookListViewBackup.getItems().setAll(bookListView.getItems());
		System.out.println(books);
		
		// hello the libraryserver
		try {
			Socket librarySocket = new Socket(InetAddress.getByName(LIBRARY_SERVER_HOST), Integer.parseInt(LIBRARY_SERVER_PORT));
			PrintWriter libraryOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(librarySocket.getOutputStream())), true);
			BufferedReader libraryIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String message = String.format("HELLO %s", user.getUsername());
			
			for (Book book : books)
				message += String.format(" %s", book.getId());
			
			System.out.println(message);
			
			libraryOut.println(message);
		} catch (IOException exception) {
			exception.printStackTrace();
		}
		
		// initialize connection with AccountingService
		try {
			String serviceName = "AccountingService";
			Registry registry = LocateRegistry.getRegistry(8089);
			accountingService = (AccountingInterface)registry.lookup(serviceName);
		} catch (NotBoundException exception) {
			logger.warning("Service name not bound in the registry, unable to connect.");
		} catch (RemoteException exception) {
			logger.severe("Could not establish connection with AccountingService: " + exception.getMessage());
		}
	}
	
	private void startMessageListener() {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                String message = fanoutReceiver.dequeueMessage();
                if (message != null && !message.isEmpty()) {
                    supplyRequestView.getItems().add(message);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warning("Message listener interrupted: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS);
    }	
	
	
	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
	
	public void search(ActionEvent actionEvent) {
		String criteria = titleFilter.getText().toLowerCase();

		if (criteria.isBlank() || criteria.isEmpty())
			return;
		
	    reset(actionEvent);
	    
	    // Create a filtered list
	    List<String> filteredItems = new ArrayList<>();

	    bookListView.getItems().forEach(bookString -> {
	        if (bookString.toLowerCase().contains(criteria)) {
	            filteredItems.add(bookString);
	        }
	    });

	    System.out.println(bookListView.getItems().size());
	    System.out.println(filteredItems.size());

	    // Update the existing ListView's items directly
	    bookListView.getItems().setAll(filteredItems);
	}

	public void reset(ActionEvent actionEvent) {
	    // Reset to the original items
	    bookListView.getItems().setAll(bookListViewBackup.getItems());
	}
	
	public void supply(ActionEvent actionEvent) {
		String bookID = supplyGutenbergID.getText();
		
		if (bookID.isBlank() || bookID.isEmpty())
			return;
		
		String response = "";
		try {
			out.println(String.format("ADD_BOOK %s", bookID));
			response = in.readLine();
		} catch (IOException exception) {
			logger.warning("Book supply request failed: " + response + ", reason: " + exception.getMessage());
		}
	}
	
	public void confirmSupplyRequest(ActionEvent actionEvent) {
		try {
			// book id
			String request = fanoutReceiver.dequeueMessage();
			
			// add book via open socket
			out.println("ADD_BOOK " + request);
			
			try {
				Book fetched = Utility.fromID(request);
				Receipt receipt = accountingService.formReceipt(Arrays.asList(fetched));
			} catch (MalformedURLException exception) {
				logger.warning("Malformed URL received: " + exception.getMessage());
			} catch (RemoteException exception) {
				logger.warning("Unable to form receipt due to a RemoteException: " + exception.getMessage());
			}
		} catch (InterruptedException exception) {
			logger.warning("Failed to dequeue message from queue: " + exception.getMessage());
		}
	}
	
	public void denySupplyRequest(ActionEvent actionEvent) {
		try {
			// book id
			String request = fanoutReceiver.dequeueMessage();
		} catch (InterruptedException exception) {
			logger.warning("Failed to dequeue message from queue: " + exception.getMessage());
		}
	}
	
	private List<Book> fetchBooks() {
		List<Book> books = new ArrayList<Book>();
		
		System.out.println(SUPPLIER_SERVER_HOST + ":" + SUPPLIER_SERVER_PORT);
		
		String response = "";
		try {
			out.println("GET_BOOKS");
			response = in.readLine();
			JSONArray jsonArray = new JSONArray(response);
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				books.add(Book.fromJson(jsonObject));
			}
		} catch (JSONException exception) {
			logger.warning("Failed to parse net.etfbl.model.Book from JSON string: " + response);
		} catch (IOException exception) {
			logger.severe(String.format(
					"Failed to receive response from socket %s:%s, reason: %s",
					SUPPLIER_SERVER_HOST , SUPPLIER_SERVER_PORT, exception.getMessage()));
		}
		
		return books;
	}
}
