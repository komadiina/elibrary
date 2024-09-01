package net.etfbl.application.gui.controllers;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import javafx.event.ActionEvent;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import net.etfbl.api.UserService;
import net.etfbl.application.gui.GUI;
import net.etfbl.application.gui.views.LibrarianGUI;
import net.etfbl.application.gui.views.LoginGUI;
import net.etfbl.application.gui.views.RegistrationGUI;
import net.etfbl.application.gui.views.SupplierGUI;
import net.etfbl.application.gui.views.UserGUI;
import net.etfbl.config.Configuration;
import net.etfbl.config.IOLogger;
import net.etfbl.middleware.Utility;
import net.etfbl.model.Role;
import net.etfbl.model.User;
import net.etfbl.model.api.LoginRequest;

public class LoginScreenController {
	private static final IOLogger logger = new IOLogger(LoginScreenController.class.getName());
	private User user;
	
	public TextField username;
	public TextField password;
	
	public void signIn(ActionEvent actionEvent) {
		// POST to UserService
		String endpoint = String.format("%s/login", UserService.API_HOST);
		LoginRequest credentials = new LoginRequest();
		
		credentials.setUsername(username.getText());
		credentials.setPassword(password.getText());
		
		try {
			// null - no headers of importance yet
			String response = Utility.processRequest(endpoint, "POST", null, credentials.toJson());
			JSONObject responseJson = new JSONObject(response);
			
			// Add received authorization token into next requests
			Configuration.requestHeaders.put("Authorization", "Bearer " + responseJson.get("authToken"));
			
			User user = User.fromJsonObject(responseJson.getJSONObject("user"));
			
			if (!user.getActivated()) {
				logger.info(String.format("User %s not activated - failed to login.", user.toString()));
				return;
			}
			
			// cheeky, but state management in javafx sucks :)
			Configuration.currentUser = user;
			
			if (user.getRole().equals(Role.LIBRARIAN)) 
				transitionToLibrarian(actionEvent);
			else if (user.getRole().equals(Role.SUPPLIER))
				transitionToSupplier(actionEvent);
			else transitionToUser(actionEvent);
		} catch (JSONException exception) {
			logger.info("No authorization token received: " + exception.getMessage());
			exception.printStackTrace();
		} catch (Exception exception) {
			logger.severe("Unexpected exception occurred: " + exception.getMessage());
			exception.printStackTrace();
		}
		
	}
	
	public void signUp(ActionEvent actionEvent) {
		// Transition to RegistrationScreen
		try {
			Stage stage = (Stage) ((javafx.scene.Node) actionEvent.getSource()).getScene().getWindow();
			GUI from = new LoginGUI(stage);
			RegistrationGUI to = new RegistrationGUI(stage);
			
			GUI.transition(from, to);
		} catch (IOException exception) {
			// TODO
		}
	}
	
	private void transitionToLibrarian(ActionEvent propagatedEvent) {
		try {
			Stage stage = (Stage) ((javafx.scene.Node) propagatedEvent.getSource()).getScene().getWindow();
			GUI from = new LoginGUI(stage);
			GUI to = new LibrarianGUI(stage, this.user);
			GUI.transition(from, to);
		} catch (IOException exception) {
			logger.info("Failed to execute GUI transition (LoginGUI -> LibrarianGUI). " + exception.getMessage());
		}
	}
	
	private void transitionToSupplier(ActionEvent propagatedEvent) {
		try {
			Stage stage = (Stage) ((javafx.scene.Node) propagatedEvent.getSource()).getScene().getWindow();
			GUI from = new LoginGUI(stage);
			GUI to = new SupplierGUI(stage, this.user);
			GUI.transition(from, to);
		} catch (IOException exception) {
			logger.info("Failed to execute GUI transition (LoginGUI -> SupplierGUI). " + exception.getMessage());
			exception.printStackTrace();
		}
	}
	
	private void transitionToUser(ActionEvent propagatedEvent) {
		try {
			Stage stage = (Stage) ((javafx.scene.Node) propagatedEvent.getSource()).getScene().getWindow();
			GUI from = new LoginGUI(stage);
			GUI to = new UserGUI(stage, this.user);
			GUI.transition(from, to);
		} catch (IOException exception) {
			logger.info("Failed to execute GUI transition (LoginGUI -> UserGUI). " + exception.getMessage());
		}
	}
}
