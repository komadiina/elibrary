package net.etfbl.application.gui.views;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.etfbl.application.gui.GUI;
import net.etfbl.application.gui.controllers.SupplierController;
import net.etfbl.config.Configuration;
import net.etfbl.model.User;

public class SupplierGUI extends GUI {
	private User user;
	private SupplierController controller;
	
	public SupplierGUI(Stage stage) throws IOException {
		super(stage);
	}
	
	public SupplierGUI(Stage stage, User user) throws IOException {
		super(stage);
		
		this.user = user;
		this.controller.setUser(user);
	}
	
	@Override
	protected void load() throws IOException {
		String path = Configuration.PROJECT_ROOT + "/resources/gui/supplier-screen.fxml";
        URL fxmlUrl = Paths.get(path).toUri().toURL();
        
		FXMLLoader loader = new FXMLLoader(fxmlUrl);
		Scene scene = new Scene(loader.load(), 600, 600);
		this.stage.setTitle("e-Library | Supplier menu");
		this.stage.setScene(scene);
		
		this.controller = loader.getController();
	}
}
