package net.etfbl.config;

import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class IOLogger {
	private Logger logger;
	private FileHandler handler;
	private boolean isIOEnabled;
	
	public IOLogger(String className) {
		isIOEnabled = true;
		logger = Logger.getLogger(className);
		
		try {
			String path = String.format("%s/logs/%s-%s.log", Configuration.PROJECT_ROOT, className, (new Date()).toString().replaceAll(" ", "_").replaceAll(":","-"));
			handler = new FileHandler(path);
			logger.addHandler(handler);
			
			SimpleFormatter formatter = new SimpleFormatter();
			handler.setFormatter(formatter);
			
			logger.info("Initialized FileHandler at " + path);
		} catch (Exception ex) {
			logger.warning("Failed to instantiate IO logger for " + className + ", defaulting to console output...");
			ex.printStackTrace();
			isIOEnabled = false;
		}
		
		logger.info(String.format("Initialized logger for %s (io=%b)", className, isIOEnabled));
	}
	
	public Logger getLogger() { return this.logger; }
	
	public void disableFileLogging() {
		this.logger.removeHandler(handler);
	}
	
	public void enableFileLogging() {
		this.logger.addHandler(handler);
	}
	
	public void info(String message) {
		logger.info(message);
	}
	
	public void fine(String message) {
		logger.fine(message);
	}
	
	public void finer(String message) {
		logger.finer(message);
	}
	
	public void finest(String message) {
		logger.finest(message);
	}
	
	public void warning(String message) {
		logger.warning(message);
	}
	
	public void severe(String message) {
		logger.severe(message);
	}
	
}
