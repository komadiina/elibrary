package net.etfbl.config;

import java.io.*;
import java.util.*;

import net.etfbl.model.User;

public class Configuration {
	public static final String PROJECT_ROOT = "C:/Users/ognjen/Desktop/projektni";
	public static Properties projectProperties = loadProperties("project");
	public static HashMap<String, String> requestHeaders = new HashMap<String, String>();
	public static User currentUser;
	
	public static final String PROPOSAL_MULTICAST_GROUP = "230.0.0.0";
	public static final int PORT = 8446;
	
	public static Properties loadProperties(String baseName) {
		Properties props = new Properties();
		String dest = String.format("%s/%s/%s.properties", PROJECT_ROOT, "resources", baseName);
		
		try (InputStream is = new FileInputStream(new File(dest));) {
			props.load(is);
		} catch (IOException exception) {
			return null;
		}
		
		return props;
	}
	
	public static String getCurrentPath() {
		return PROJECT_ROOT;
	}
}
