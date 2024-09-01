package net.etfbl.application;

import java.io.File;

import javafx.application.Application;

import javafx.stage.Stage;
import net.etfbl.application.gui.GUI;
import net.etfbl.application.gui.views.LoginGUI;
import net.etfbl.config.Configuration;

public class Main extends Application {
	@Override
	public void start(Stage arg0) throws Exception {
		GUI window = new LoginGUI(arg0);
		window.show();
	}
	
	public static void main(String[] args) {
		System.setProperty("java.security.policy", 
				Configuration.PROJECT_ROOT + File.separator +
				Configuration.projectProperties.getProperty("rmiPolicyFile"));
		
//		if (System.getSecurityManager() == null)
//			System.setSecurityManager(new SecurityManager());
		
		launch();
	}	
}