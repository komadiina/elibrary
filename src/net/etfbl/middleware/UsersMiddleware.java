package net.etfbl.middleware;

import java.io.*;
import java.util.*;

//import org.slf4j.*;

import net.etfbl.config.Configuration;
import net.etfbl.config.IOLogger;
import net.etfbl.model.*;
import net.etfbl.model.serial.*;

public class UsersMiddleware implements AutoCloseable {
	private static final IOLogger logger = new IOLogger(UsersMiddleware.class.getName());
	
	private static UsersMiddleware instance = null;
	
	public List<User> users;
	
	public UsersMiddleware() {
		users = _loadFromFS();
	}
	
	public UsersMiddleware(boolean loadDemoData) {
		this();
		
		if (loadDemoData)
			_initDemoData();
	}
	
	private void _initDemoData() {
		logger.info("Loading demo data...");
		
		try {
			Serializer<List<User>> s = new XMLSerializer<List<User>>();
			users = s.deserialize(String.format(
						"%s/%s", 
						Configuration.PROJECT_ROOT,
						Configuration.projectProperties.getProperty("demoUsersStore")
					));
		} catch (IOException ex) {
			logger.warning("Failed to load demo data.");
		}
		
		logger.info("Finished initializing demo data.");
	}
	
	private List<User> _loadFromFS() {
		Serializer<List<User>> serializer = new XMLSerializer<List<User>>();
		String path = String.format("%s/%s", 
				Configuration.PROJECT_ROOT, 
				Configuration.projectProperties.getProperty("usersStore"));
		List<User> read = new ArrayList<User>();
		
		try {
			read = (List<User>)serializer.deserialize(path);
		} catch (IOException exception) {
			logger.warning("Failed to deserialize: " + path);
		}
		
		return read;
	}
	
	// Allow possibility to be used as a singleton
	public static UsersMiddleware getInstance(boolean useDemoData) {
		if (instance == null)
			if (useDemoData)
				instance = new UsersMiddleware(true);
			else instance = new UsersMiddleware(false);
		
		return instance;
	}
	
	public List<User> getAll() {
		logger.info("getAll()");
		return this.users; 
	}
	
	public User get(String email) {
		logger.info(String.format("get(%s)", email));
		
		for (User user : users) 
			if (user.getEmail().equals(email))
				return user;
		
		return null;
	}
	
	public User getUsername(String username) {
		logger.info(String.format("getUsername(%s)", username));
		
		for (User user : users)
			if (user.getUsername().equals(username))
				return user;
		
		return null;
	}
	
	public boolean add(User user) {
		logger.info(String.format("add(%s)", user.toString()));
		
		if (users.contains(user)) {
			logger.info("false");
			return false;
		}
		
		users.add(user);
		logger.info("true");
		
		persistCollection();
		return true;
	}
	
	public boolean update(User user, String email) {
		logger.info(String.format("update(%s, %s)", user.toString(), email));
		
		int index = -1;
		for (int i = 0; i < users.size(); i++)
			if (users.get(i).getEmail().equals(email)) {
				index = i;
				break;
			}
		
		if (index == -1)
			return false;
		
		logger.info(String.format("true, %d", index));
		users.set(index, user);
		
		persistCollection();
		return true;
			
	}
	
	public boolean delete(User user) {
		logger.info(String.format("delete(%s)", user.toString()));
		users.remove(user);
		
		persistCollection();
		return true;
	}

	public boolean register(User user) {
		logger.info(String.format("register(%s)", user.toString()));
		return add(user);
	}
	
	public void activateAccount(User user) {
		// update in this.users? TODO
		logger.info(String.format("activateAccount(%s)", user.toString()));
		user.setActivated(true);
		persistCollection();
	}
	
	public void deactivateAccount(User user) {
		// update in this.users? TODO
		logger.info(String.format("deactivateAccount(%s)", user.toString()));
		user.setActivated(false);
		persistCollection();
	}

	public void persistCollection() {
		logger.info("persistCollection()");
		Serializer<List<User>> serializer = new XMLSerializer<List<User>>();
		String path = String.format("%s/%s", 
				Configuration.PROJECT_ROOT, 
				Configuration.projectProperties.getProperty("usersStore"));
		
		try {
			serializer.serialize(users, path);
		} catch (Exception ex) {
			logger.severe(String.format("Failed to serialize object [%s] at %s.", users.toString(), path));
		}	
	}
	
	@Override
	public void close() throws Exception {
		logger.info("AutoCloseable:close()");
		persistCollection();
	}
}
