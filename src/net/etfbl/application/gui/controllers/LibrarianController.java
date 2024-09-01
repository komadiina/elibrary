package net.etfbl.application.gui.controllers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javafx.collections.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import net.etfbl.api.UserService;
import net.etfbl.config.Configuration;
import net.etfbl.config.IOLogger;
import net.etfbl.middleware.BooksMiddleware;
import net.etfbl.middleware.MulticastListener;
import net.etfbl.middleware.UsersMiddleware;
import net.etfbl.middleware.Utility;
import net.etfbl.model.*;
import net.etfbl.model.api.RequestResponse;
import net.etfbl.model.rmi.AccountingInterface;
import net.etfbl.mq.LibrarianFanoutSender;
import net.etfbl.servers.LibraryServer;

public class LibrarianController implements Initializable {
	private static final IOLogger logger = new IOLogger(LibrarianController.class.getName());
	
	private static final String LIBRARY_SERVER_HOST = "localhost";
	private static final String LIBRARY_SERVER_PORT = Configuration.projectProperties.getProperty("libraryServerPort");
	
	private User user = Configuration.currentUser;
	
	// for member <-> librarian communication
	private MulticastSocket multicastSocket;
	private MulticastListener multicastListener;
	
	// for librarian <-=> supplier communication
	private LibrarianFanoutSender librarianFanoutSender;
	
	@FXML
	private ListView<String> userListView;
	private ObservableSet<String> observableSet;
	private List<User> usersFetched;
	
	public TextField userSelectionEmail;
	
	public ListView<String> supplierNames;
	// fmt: User oggnjen requested Gutenberg book ID: 24062.
	public Label bookProposalNotification;
	
	public TextField supplierID;
	public TextField supplierBookID;
	
	// fmt: (24602) Supplier oggnjen response: Approved. VAT expenses: 140.99$
	public Label supplierResponse;
	
	private String activeProposal = "";

	private Thread vatWatcher;
	public Label vatAmountLabel;
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		userListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		observableSet = FXCollections.observableSet();
		
		observableSet.addListener((SetChangeListener.Change<? extends String> c) -> {
			if (c.wasAdded())
				userListView.getItems().add(c.getElementAdded());
			if (c.wasRemoved())
				userListView.getItems().remove(c.getElementRemoved());
		});
		
		List<String> usersStrings = usersToStrings();
		observableSet.addAll(usersStrings);
		
		supplierNames.getItems().addAll(LibraryServer.suppliers);
		
		try {
			multicastSocket = new MulticastSocket(Configuration.PORT);
			InetAddress group = InetAddress.getByName(Configuration.PROPOSAL_MULTICAST_GROUP);
			multicastListener = new MulticastListener(multicastSocket, group, this::processUserRequest);
			multicastListener.start();
			librarianFanoutSender = new LibrarianFanoutSender();
			logger.info("Started multicast listener.");
        } catch (IOException e) {
            logger.severe("Failed to join multicast group: " + e.getMessage());
        }
		
		initSuppliers();
		
		vatWatcher = new Thread() {
			@Override
			public void run() {
				synchronized (LibraryServer.bell) {
					try {
						LibraryServer.bell.wait();
						
						vatAmountLabel.setText(String.format("VAT to pay: %.2f", LibraryServer.recentVAT));
						Thread.sleep(3000);
						vatAmountLabel.setText("");
					} catch (InterruptedException exception) {
						logger.info("vatWatcher: InterruptedException, stopping...");
					}
				}
				
			}
		};
		
		vatWatcher.start();
	}
	
	private void initSuppliers() {
		try {
			Socket librarySocket = new Socket(InetAddress.getByName(LIBRARY_SERVER_HOST), Integer.parseInt(LIBRARY_SERVER_PORT));
			PrintWriter libraryOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(librarySocket.getOutputStream())), true);
			BufferedReader libraryIn = new BufferedReader(new InputStreamReader(librarySocket.getInputStream()));
			String message = "GET_SUPPLIERS";
			libraryOut.println(message);
			
			// JSON Serialized as {supplier: name, books: [...]}
			String responseJson = libraryIn.readLine();
	        JSONObject jsonResponse = new JSONObject(responseJson);
	        for (String supplier : jsonResponse.keySet()) {
	            JSONArray booksArray = jsonResponse.getJSONArray(supplier);
	            List<String> books = new ArrayList<>();
	            for (int i = 0; i < booksArray.length(); i++) {
	                books.add(booksArray.getString(i));
	            }
	            LibraryServer.supplierOffer.put(supplier, books);
	        }

	        librarySocket.close();
		} catch (IOException exception) {
			exception.printStackTrace();
		}
		
		System.out.println(LibraryServer.supplierOffer);
		
		for (Map.Entry<String, List<String>> entry : LibraryServer.supplierOffer.entrySet()) {
			for (String bookID : entry.getValue())
			{
				try {
					Book book = Utility.fromID(bookID);			
					supplierNames.getItems().add(String.format("[%s] %s", entry.getKey(), book.prettyString())); 
				} catch (MalformedURLException exception) {
					logger.warning("Received a malformed URL: " + exception.getMessage());
					continue;
				}
			}
		}
		
		supplierNames.refresh();
	}
	
	private String processUserRequest(String request) {
		if (!request.startsWith("REQUEST_BOOK"))
			return "Unknown request.";
		
		String bookID = request.trim().split(" ")[1];
		activeProposal = bookID;
		String requester = "<unknown>";
		try {
			requester = request.trim().split(" ")[2];
		} catch (ArrayIndexOutOfBoundsException exception) {}
		
		bookProposalNotification.setText(String.format("Book (ID #%s) has been proposed by %s.", bookID, requester));
		return "Book proposal under review.";
	}
	
	public void approveBookProposal(ActionEvent actionEvent) {
        int proposalIndex = getProposalIndex();
        if (proposalIndex >= 0) {
            BooksMiddleware.getInstance().reviewProposal(proposalIndex, true);
            bookProposalNotification.setText("Book proposal approved.");
        } else {
            bookProposalNotification.setText("No proposal selected.");
        }
    }

    public void denyBookProposal(ActionEvent actionEvent) {
        int proposalIndex = getProposalIndex();
        if (proposalIndex >= 0) {
            BooksMiddleware.getInstance().reviewProposal(proposalIndex, false);
            bookProposalNotification.setText("Book proposal denied.");
        } else {
            bookProposalNotification.setText("No proposal selected.");
        }
    }
    
    private int getProposalIndex() {
    	int index = -1;
    	
    	for (int i = 0; i < BooksMiddleware.getInstance().proposals.size(); i++) {
    		BookProposal proposal = BooksMiddleware.getInstance().proposals.get(i);
    		if (proposal.getBookID().equals(activeProposal))
    			return i;
    	}
    	
    	return index;
    }
    
	public void requestSupply(ActionEvent actionEvent) {
		String bookID = supplierBookID.getText();
		
		if (bookID.isBlank() || bookID.isEmpty()) 
			return;
		
		try {
			librarianFanoutSender.queueMessage(bookID);
		} catch (Exception exception) {
			logger.info("Could not queue message: " + exception.getMessage());
			exception.printStackTrace();
		}
	}
	
	public void shutdown() {
        if (multicastSocket != null) {
            try {
                InetAddress group = InetAddress.getByName(Configuration.PROPOSAL_MULTICAST_GROUP);
                multicastSocket.leaveGroup(group);
                multicastSocket.close();
                logger.info("Left multicast group: " + Configuration.PROPOSAL_MULTICAST_GROUP);
            } catch (IOException e) {
                logger.severe("Failed to leave multicast group: " + e.getMessage());
            }
        }
    }
	
	
	private List<String> usersToStrings() {
		return UsersMiddleware.getInstance(false).getAll()
				.stream()
				.map(x -> String.format(
						"[%s, %s] %s %s - %s, %s",
						x.getUsername(),
						x.getEmail(),
						x.getFirstName(),
						x.getLastName(),
						x.getRole().toString(),
						x.getActivated() ? "Active!" : "Inactive."
						))
				.toList();
	}
	
	private void refreshObservable() {
		List<String> usersStrings = usersToStrings();
		observableSet.clear();
		observableSet.addAll(usersStrings);
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
	
	public void setUsersFetched(List<User> userList) {
		this.usersFetched = userList;
		userListView.refresh();
//		updateUserListView();
	}
	
	public List<User> getUsersFetched() {
		return usersFetched;
	}

	public void activate(ActionEvent actionEvent) {
		logger.info("Activate user request by " + user.toString());
		String email = userSelectionEmail.getText();
		
		if (email.isEmpty())
			return;

		String response = "";
		JSONObject responseJson = null;
		try {
			String endpoint = String.format("%s/activate?email=%s", UserService.API_HOST, email);
			String method = "POST";
			
			response = Utility.processRequest(endpoint, method, Configuration.requestHeaders, this.user.toJson());
			responseJson = new JSONObject(response);
			
			// Returns User object on success
			User result = User.fromJsonObject(responseJson);
			refreshObservable();
		} catch (JSONException exception) {
			// Returned object is not of type 'User'
			if (responseJson == null)
				return;
			
			try {
				RequestResponse requestResponse = RequestResponse.fromJsonObject(responseJson);
				logger.warning(response.toString());
			} catch (JSONException parseException) {
				logger.severe("Unknown JSONException exception caught: " + parseException.getMessage());
			}
		} catch (Exception exception) {
			logger.severe("Could not activate user, unknown exception occurred: " + exception.getMessage());
		}
	}
	
	public void deactivate(ActionEvent actionEvent) {
		logger.info("Dectivate user request by " + user.toString());
		String email = userSelectionEmail.getText();
		
		if (email.isEmpty())
			return;

		String response = "";
		JSONObject responseJson = null;
		try {
			String endpoint = String.format("%s/deactivate?email=%s", UserService.API_HOST, email);
			String method = "POST";
			
			response = Utility.processRequest(endpoint, method, Configuration.requestHeaders, this.user.toJson());
			responseJson = new JSONObject(response);
			
			// Returns User object on success
			User result = User.fromJsonObject(responseJson);
			refreshObservable();
		} catch (JSONException exception) {
			// Returned object is not of type 'User'
			if (responseJson == null)
				return;
			
			try {
				RequestResponse requestResponse = RequestResponse.fromJsonObject(responseJson);
				logger.warning(response.toString());
			} catch (JSONException parseException) {
				logger.severe("Unknown JSONException exception caught: " + parseException.getMessage());
			}
		} catch (Exception exception) {
			logger.severe("Could not activate user, unknown exception occurred: " + exception.getMessage());
		}
	}
	
	public void delete(ActionEvent actionEvent) {
		logger.info(String.format(
				"Delete user (%s) request by %s", 
				userSelectionEmail.getText(), 
				Configuration.currentUser.toString()));
		String email = userSelectionEmail.getText();
		
		if (email.isEmpty() || email.isBlank())
			return;
		
		String response = "";
		JSONObject responseJson = null;
		
		try {
			String endpoint = String.format("%s/remove?email=%s", UserService.API_HOST, email);
			String method = "POST";
			
			response = Utility.processRequest(endpoint, method, Configuration.requestHeaders, this.user.toJson());
			responseJson = new JSONObject(response);
			
			// Returns User object on success
			User result = User.fromJsonObject(responseJson);
			refreshObservable();
		} catch (JSONException exception) {
			// Returned object is not of type 'User'
			if (responseJson == null)
				return;
			
			try {
				RequestResponse requestResponse = RequestResponse.fromJsonObject(responseJson);
				logger.warning(response.toString());
			} catch (JSONException parseException) {
				logger.severe("Unknown JSONException exception caught: " + parseException.getMessage());
			}
		} catch (Exception exception) {
			logger.severe("Could not delete user, unknown exception occurred: " + exception.getMessage());
		}
	}
}
