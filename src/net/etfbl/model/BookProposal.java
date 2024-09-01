package net.etfbl.model;

import java.io.Serializable;

public class BookProposal implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String bookID;
    private String bookTitle;
    private String author;
    private String memberId;
    private boolean approved;

    public BookProposal(String id, String bookTitle, String author, String memberId) {
    	this.bookID = id;
        this.bookTitle = bookTitle;
        this.author = author;
        this.memberId = memberId;
        this.approved = false;
    }
    
    public String getBookID() {
    	return bookID;
    }
    
    public void setBookID(String id) {
    	this.bookID = id;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public String getAuthor() {
        return author;
    }

    public String getMemberId() {
        return memberId;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    @Override
    public String toString() {
        return "Proposal [bookTitle=" + bookTitle + ", author=" + author + ", memberId=" + memberId + ", approved=" + approved + "]";
    }
}