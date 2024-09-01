package net.etfbl.application.gui.views;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.etfbl.application.gui.GUI;
import net.etfbl.application.gui.controllers.LoginScreenController;
import net.etfbl.config.Configuration;

public class LoginGUI extends GUI {
	public LoginScreenController controller;
	
	public LoginGUI(Stage stage) throws IOException {
		super(stage);
	}
	
	@Override
	protected void load() throws IOException {
        String path = Configuration.PROJECT_ROOT + "/resources/gui/login-screen.fxml";
        URL fxmlUrl = Paths.get(path).toUri().toURL();
        
		FXMLLoader loader = new FXMLLoader(fxmlUrl);
		Scene scene = new Scene(loader.load(), 600, 300);
		this.stage.setTitle("e-Library | Librarian menu");
		this.stage.setScene(scene);
		
		this.controller = loader.getController();
	}
}
