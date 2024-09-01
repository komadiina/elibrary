package net.etfbl.model.api;

public class DownloadRequest {
	String mailTo;
	String bookID;
	
	public DownloadRequest() {
		super();
	}
	
	public DownloadRequest(String mailTo, String bookID) {
		super();
		this.mailTo = mailTo;
		this.bookID = bookID;
	}

	public String getMailTo() {
		return mailTo;
	}

	public void setMailTo(String mailTo) {
		this.mailTo = mailTo;
	}

	public String getBookID() {
		return bookID;
	}

	public void setBookID(String bookID) {
		this.bookID = bookID;
	}
	
	public String toJson() {
		return String.format(
				"{\"mailTo\": \"%s\","
				+ "\"bookID\": \"%s\"}",
				mailTo,
				bookID
				);
	}
}
