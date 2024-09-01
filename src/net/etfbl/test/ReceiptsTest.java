package net.etfbl.test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.etfbl.config.Configuration;
import net.etfbl.middleware.BooksMiddleware;
import net.etfbl.model.Book;
import net.etfbl.model.Receipt;
import net.etfbl.model.serial.Serializer;
import net.etfbl.model.serial.XMLSerializer;

public class ReceiptsTest {
	public static void main(String[] args) {
		List<Book> books = BooksMiddleware.getInstance().getAll();
		
		Receipt r1 = new Receipt();
		r1.setBooks(books);
		r1.setDate(new Date());
		r1.setPrice(Receipt.calculatePrice(r1));
		r1.setVat(r1.getPrice() * 0.17);
		
		books.remove(1);
		Receipt r2 = new Receipt();
		r2.setBooks(books);
		r2.setDate(new Date());
		r2.setPrice(Receipt.calculatePrice(r2));
		r2.setVat(r2.getPrice() * 0.17);
		
		Set<Receipt> receipts = new HashSet<Receipt>(Arrays.asList(r1, r2));
		System.out.println(receipts);
		
		System.out.println("Serializing...");
		Serializer<Set<Receipt>> serializer = new XMLSerializer<Set<Receipt>>();
		String path = Configuration.PROJECT_ROOT + "/" + Configuration.projectProperties.getProperty("receiptsStore");
		try {
			serializer.serialize(receipts, path);
			System.out.println("Successfully serialized.");
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		
		System.out.println("Deserializing...");
		Set<Receipt> receiptsDeserialized;
		try {
			receiptsDeserialized = serializer.deserialize(path);
			System.out.println("Successfully deserialized.");
			System.out.println(receiptsDeserialized);
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		
	}
}
