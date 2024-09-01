package net.etfbl.model;

import java.io.Serializable;

public class Order implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1506416255637484810L;
	
	private User librarian;
	private Book toOrder;
	private Integer amount;
	private User supplier;
	private Boolean approved = null;
	
	public Order(User librarian, Book toOrder, Integer amount, User supplier, Boolean approved) {
		super();
		this.librarian = librarian;
		this.toOrder = toOrder;
		this.amount = amount;
		this.supplier = supplier;
		this.approved = approved;
	}
	
	public Order() {}

	public User getLibrarian() {
		return librarian;
	}

	public void setLibrarian(User librarian) {
		this.librarian = librarian;
	}

	public Book getToOrder() {
		return toOrder;
	}

	public void setToOrder(Book toOrder) {
		this.toOrder = toOrder;
	}

	public Integer getAmount() {
		return amount;
	}

	public void setAmount(Integer amount) {
		this.amount = amount;
	}

	public User getSupplier() {
		return supplier;
	}

	public void setSupplier(User supplier) {
		this.supplier = supplier;
	}

	public Boolean getApproved() {
		return approved;
	}

	public void setApproved(Boolean approved) {
		this.approved = approved;
	}

	@Override
	public String toString() {
		return "Order [librarian=" + librarian + ", toOrder=" + toOrder + ", amount=" + amount + ", supplier="
				+ supplier + ", approved=" + approved + "]";
	}
}
