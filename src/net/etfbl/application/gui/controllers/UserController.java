package net.etfbl.application.gui.controllers;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;

import org.json.JSONException;
import org.json.JSONObject;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import net.etfbl.api.BookService;
import net.etfbl.config.Configuration;
import net.etfbl.config.IOLogger;
import net.etfbl.middleware.BooksMiddleware;
import net.etfbl.middleware.MulticastListener;
import net.etfbl.middleware.SSLContextUtil;
import net.etfbl.middleware.Utility;
import net.etfbl.model.Book;
import net.etfbl.model.BookProposal;
import net.etfbl.model.User;
import net.etfbl.model.api.DownloadRequest;
import net.etfbl.servers.MemberChatClient;

public class UserController implements Initializable {
	private static final IOLogger logger = new IOLogger(UserController.class.getName());
	
	private User user = Configuration.currentUser;
	private MulticastSocket multicastSocket;
	private MulticastListener multicastListener;
	
	@FXML
	public ListView<String> bookListView;
	private ListView<String> bookListViewBackup = new ListView<String>();
	
	private ObservableSet<String> observableSet;
	
	// search()
	public TextField titleFilter;
	
	// suggest()
	public TextField suggestGutenbergID;
	public Label bookProposalResponse;
	
	
	// download(), details()
	public TextField selectGutenbergBookID;
	
	
	// chat
	private MemberChatClient memberChatClient;
    private ExecutorService chatExecutor = Executors.newSingleThreadExecutor();
	public ListView<String> chatListView;
	public TextField chatInput;
	public Button sendButton;
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		this.user = Configuration.currentUser;
		bookListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		observableSet = FXCollections.observableSet();
		
		observableSet.addListener((SetChangeListener.Change<? extends String> c) -> {
			if (c.wasAdded())
				bookListView.getItems().add(c.getElementAdded());
			if (c.wasRemoved())
				bookListView.getItems().remove(c.getElementRemoved());
		});
		
		List<String> bookStrings = booksToStrings();
		System.out.println(bookStrings);
		
		observableSet.addAll(bookStrings);
		bookListView.getItems().setAll(bookStrings);
		bookListViewBackup.getItems().setAll(bookStrings);
		
		this.user = Configuration.currentUser;
		
		try {
			multicastSocket = new MulticastSocket(Configuration.PORT);
			InetAddress group = InetAddress.getByName(Configuration.PROPOSAL_MULTICAST_GROUP);
			multicastListener = new MulticastListener(multicastSocket, group, this::processLibrarianResponse);
			multicastListener.start();
			logger.info("Started multicast listener.");
		} catch (IOException exception) {
			logger.severe("Failed to instantiate multicastListener: " + exception.getMessage());
		}
		
		initializeChatClient();
		this.user = Configuration.currentUser;
		
		System.out.println(this.user);
	}
	
	private void initializeChatClient() {
		try {
			SSLContext context = SSLContextUtil.getSSLContext();
			memberChatClient = new MemberChatClient(user, context, this::handleIncomingMessage);
            chatExecutor.submit(() -> {
                try {
                    memberChatClient.start();
                } catch (IOException e) {
                    logger.severe("Error starting MemberChatClient: " + e.getMessage());
                }
            });
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}
	
	private void handleIncomingMessage(String message) {
        // Update the chatListView with the new message
		Platform.runLater(() -> chatListView.getItems().add(message));
    }
	
    public void sendMessage(ActionEvent event) {
        String message = chatInput.getText();
        if (message.isEmpty()) {
            return;
        }
        
        final String formatted = String.format("[%s] %s", Configuration.currentUser.getUsername(), message);
        Platform.runLater(() -> {
        	chatListView.getItems().add(formatted);
        });

        // this.user == null ???
        memberChatClient.sendMessage(formatted);
        chatInput.clear();
    }
	
	private String processLibrarianResponse(String response) {
		return "TODO: processLibrarianResponse";
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

	
	public void suggest(ActionEvent actionEvent) {
		String text = suggestGutenbergID.getText();
		
		if (text.isBlank() || text.isEmpty())
			return;
		
		String[] tokens = text.split(" ");
		if (tokens.length > 1) 
			suggestMultiple(actionEvent, (List<String>)(new ArrayList<String>(Arrays.asList(tokens))));
		else suggestOne(actionEvent, text);
	}
	
	public void details(ActionEvent actionEvent) {
		String id = selectGutenbergBookID.getText();
		
		// fetch book structure via id
		Book book = fetchBookByID(id);
		
		// fetch book content via book object
		List<String> bookContent = new ArrayList<String>();
		try {
			Integer numLines = 100;
			bookContent = Files.readAllLines(Paths.get(book.getFilePath()))
					.stream()
					.limit(numLines)
					.toList();
		} catch (IOException exception) {
			logger.severe("Could not fetch book content: " + exception.getMessage());
			return;
		}
		
		showAlert(String.format("%s by %s", book.getTitle(), book.getAuthor()), bookContent.toString());
	}
	
	public void download(ActionEvent actionEvent) {
		this.user = Configuration.currentUser;
		
		String input = selectGutenbergBookID.getText();
		String[] processed = input.trim().replaceAll(" ", "").split(",");
		
		if (processed.length > 1) {
			for (String id : processed)
				downloadOne(id, actionEvent);
		} else {
			downloadOne(input, actionEvent);
		}
	}
	
	private void downloadOne(String input, ActionEvent propagatedEvent) {
		// fetch book structure via id
		Book book = fetchBookByID(input);
		String endpoint = String.format("%s/download", BookService.API_HOST);
		final String method = "POST";
		DownloadRequest body = new DownloadRequest(user.getEmail(), book.getId());
		String response = "";
		try {
			response = Utility.processRequest(endpoint, method, null, body.toJson());
			System.out.println(response);
			logger.info(response);
		} catch (Exception exception) {
			logger.severe("Could not request /api/books/download, reason: " + exception.getMessage());
		}
	}
	
	private void suggestOne(ActionEvent actionEvent, String id) {
		try {
			Book requestBook = Book.fromLink(
					new URL(String.format("https://www.gutenberg.org/cache/epub/%s/pg%s.txt", id, id)));
			BooksMiddleware.getInstance().submitProposal(
					new BookProposal(id, requestBook.getTitle(), requestBook.getAuthor(), user.getUsername()));
		} catch (MalformedURLException exception) {
			logger.warning("Malformed URL: " + exception.getMessage());
		}
	}
	
	private void suggestMultiple(ActionEvent actionEvent, List<String> ids) {
		for (String id : ids)
			suggestOne(actionEvent, id);
	}
	
	private Book fetchBookByID(String id) {
		logger.info("Fetch book request: " + id);
		Book result = null;
		
		String response = "";
		JSONObject responseJson = null;
		
		try {
			String endpoint = String.format("%s/find?id=%s", BookService.API_HOST, id);
			final String method = "GET";
			response = Utility.processRequest(endpoint, method, Configuration.requestHeaders, null);
			responseJson = new JSONObject(response);
			result = Book.fromJson(responseJson);
		} catch (JSONException exception) {
			logger.warning("Could not parse response as JSON: " + response + ", " + exception.getMessage());
		} catch (Exception exception) {
			logger.severe("Unknown exception occurred: " + exception.getMessage());
		}
		
		return result;
	}
	
	private List<String> booksToStrings() {
		return BooksMiddleware.getInstance().books.stream().map(x -> x.prettyString()).toList();
	}
	
	private void showAlert(String title, String message) {
		Alert alert = new Alert(AlertType.INFORMATION);
		
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(message);
		
		alert.showAndWait();
	}
}
