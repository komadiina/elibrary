package net.etfbl.application.gui.controllers;


import org.json.JSONObject;

import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import net.etfbl.api.UserService;
import net.etfbl.config.IOLogger;
import net.etfbl.middleware.Utility;
import net.etfbl.model.api.RegistrationRequest;

public class RegistrationScreenController {
	private static final IOLogger logger = new IOLogger(RegistrationScreenController.class.getName());
	
	public TextField username;
	public TextField email;
	public TextField firstName;
	public TextField lastName;
	public TextField password;
	public TextField password2;

	public Label registerStatus;
	
	public void register(ActionEvent actionEvent) {
		try {
			String endpoint = String.format("%s/register", UserService.API_HOST);
			String method = "POST";
			RegistrationRequest requestDetails = new RegistrationRequest(
						username.getText(),
						password.getText(),
						password2.getText(),
						firstName.getText(),
						lastName.getText(),
						email.getText()
					);
			System.out.println(requestDetails.toJson());
			String response = Utility.processRequest(endpoint, method, null, requestDetails.toJson());
			JSONObject responseJson = new JSONObject(response);
			System.out.println(responseJson.toString());
		} catch (Exception exception) {
			logger.warning(exception.getMessage());
		}
	}
}
