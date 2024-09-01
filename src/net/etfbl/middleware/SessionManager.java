package net.etfbl.middleware;

import java.util.*;

public class SessionManager {
	// <username, token>
    private Map<String, String> sessionStore = new HashMap<>();
    private static final SessionManager instance = new SessionManager();

    private SessionManager() {}

    public static SessionManager getInstance() {
        return instance;
    }

    public String generateToken(String username) {
        String token = UUID.randomUUID().toString();
        sessionStore.put(username, token);
        return token;
    }

    public boolean isAuthenticated(String username) {
    	System.out.println(sessionStore);
        return sessionStore.containsKey(username);
    }
    
    public boolean isTokenValid(String token) {
    	System.out.println(sessionStore);
    	return sessionStore.containsValue(token);
    }
    
    public void invalidateToken(String username) {
        sessionStore.remove(username);
    }
    
    public String getToken(String username) {
    	return sessionStore.get(username);
    }
    
    public static synchronized String authenticateUser(String username) {
    	return instance.generateToken(username);
    }
}