package net.etfbl.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class Receipt implements Serializable {
	private List<Book> books;
	private Date date;
	private double price;
	private double vat;
	
	public Receipt(List<Book> books, Date date, double price) {
		super();
		this.books = books;
		this.date = date;
		this.price = price;
	}
	
	public Receipt() {
		this.books = new ArrayList<Book>();
		this.date = new Date();
	}

	@Override
	public String toString() {
		return "Receipt [books=" + books + ", date=" + date + ", price=" + price + ", vat=" + vat + "]";
	}

	public List<Book> getBooks() {
		return books;
	}

	public void setBooks(List<Book> books) {
		this.books = books;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public double getVat() {
		return vat;
	}

	public void setVat(double vat) {
		this.vat = vat;
	}
	
	public Double calculateVat() {
		this.vat = this.price * 0.17;
		return this.vat;
	}
	
	public static Double calculatePrice(Receipt receipt) {
		Random rng = new Random();
		
		return receipt.getBooks().stream()
				.mapToDouble(book -> {
					int basePrice = 19 + rng.nextInt(30);
					return basePrice + 0.99;
				})
				.sum();
	}
}
