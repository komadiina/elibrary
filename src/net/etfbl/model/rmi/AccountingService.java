package net.etfbl.model.rmi;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import net.etfbl.config.Configuration;
import net.etfbl.config.IOLogger;
import net.etfbl.model.Book;
import net.etfbl.model.Receipt;
import net.etfbl.model.serial.Serializer;
import net.etfbl.model.serial.XMLSerializer;

public class AccountingService implements AccountingInterface {
	private static final IOLogger logger = new IOLogger(AccountingService.class.getName());
	private final String receiptPath = String.format("%s/%s",
				Configuration.PROJECT_ROOT,
				Configuration.projectProperties.getProperty("receiptsStore")
				);
	
	private Set<Receipt> receipts = new HashSet<Receipt>();
	
	public AccountingService() throws RemoteException {
		loadReceipts();
	}
	
	@Override
	public Receipt formReceipt(List<Book> books) throws RemoteException {
		Random rnd = new Random();

		// Books range from 19.99 to 49.99
		Double totalPrice = books.stream()
				.mapToDouble(book -> {
					int basePrice = 19 + rnd.nextInt(30);
					return basePrice + 0.99;
				})
				.sum();
		
		Receipt receipt = new Receipt();

		receipt.setBooks(books);
		receipt.setPrice(totalPrice);
		receipt.setVat(totalPrice * 0.17);
		receipt.setDate(new Date());
		
		receipts.add(receipt);
		saveReceipts();
		
		return receipt;
	}
	
	private void loadReceipts() {
		try {
			Serializer<Set<Receipt>> serializer = new XMLSerializer<Set<Receipt>>();
			this.receipts = serializer.deserialize(receiptPath);
		} catch (IOException exception) {
			logger.severe("Failed to deserialize receipts from " + receiptPath + ": " + exception.getMessage());
			logger.info("Creating empty receipts store...");
			this.receipts = new HashSet<Receipt>();
		}
	}
	
	private void saveReceipts() {
		try {
			Serializer<Set<Receipt>> serializer = new XMLSerializer<Set<Receipt>>();
			serializer.serialize(this.receipts, receiptPath);
		} catch (IOException exception) {
			logger.severe(String.format(
					"Failed to serialize receipts (%s) to %s: %s",
					this.receipts,
					this.receiptPath,
					exception.getMessage()
					));
		}
	}
	
	@SuppressWarnings({ "removal", "deprecation" })
	public static void main(String[] args) {
		System.setProperty(
				"java.security.policy", 
				String.format("%s/%s", 
						Configuration.PROJECT_ROOT, 
						Configuration.projectProperties.getProperty("rmiPolicyFile")));
		
//		if (System.getSecurityManager() == null) 
//			System.setSecurityManager(new SecurityManager());
		
		try {
			AccountingService server = new AccountingService();
			AccountingInterface stub = (AccountingInterface)UnicastRemoteObject.exportObject(server, 0);
			Registry registry = LocateRegistry.createRegistry(8089);
			registry.rebind("AccountService", stub);
		} catch (RemoteException exception) {
			logger.severe("Failed to instantiate RMI server: " + exception.getMessage());
		}
	}
}
