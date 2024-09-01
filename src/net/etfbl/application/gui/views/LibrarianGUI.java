package net.etfbl.application.gui.views;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.etfbl.application.gui.GUI;
import net.etfbl.application.gui.controllers.LibrarianController;
import net.etfbl.config.Configuration;
import net.etfbl.middleware.UsersMiddleware;
import net.etfbl.model.User;

public class LibrarianGUI extends GUI {
	private User user;
	private LibrarianController controller;
	
	public LibrarianGUI(Stage stage) throws IOException {
		super(stage);
	}
	
	public LibrarianGUI(Stage stage, User user) throws IOException {
		super(stage);
		this.user = user;
	}
	
	@Override
	protected void load() throws IOException {
        String path = Configuration.PROJECT_ROOT + "/resources/gui/librarian-screen.fxml";
        URL fxmlUrl = Paths.get(path).toUri().toURL();
		
		FXMLLoader loader = new FXMLLoader(fxmlUrl);
		Scene scene = new Scene(loader.load(), 600, 600);
		this.stage.setTitle("e-Library | Librarian menu");
		this.stage.setScene(scene);
		
		this.controller = loader.getController();
		this.controller.setUsersFetched(UsersMiddleware.getInstance(false).getAll());
	}
}
