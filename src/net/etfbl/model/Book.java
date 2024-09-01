package net.etfbl.model;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import net.etfbl.config.*;

public class Book {
	private static final IOLogger logger = new IOLogger(Book.class.getName());
	
	private String id;
	private String title;
	private String author;
	private String date;
	private String language;
	private String coverPage;
	private String filePath;	

	public Book(String id, String title, String author, String date, String language, String coverPage,
			String filePath) {
		super();
		this.id = id;
		this.title = title;
		this.author = author;
		this.date = date;
		this.language = language;
		this.coverPage = coverPage;
		this.filePath = filePath;
	}

	public Book(String title, String author, String date, String language, String coverPage, String filePath) {
		super();
		this.title = title;
		this.author = author;
		this.date = date;
		this.language = language;
		this.coverPage = coverPage;
		this.filePath = filePath;
	}
	
	public Book() {
		super();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Book) 
			return this.id.equals(((Book)obj).id);

		return false;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getCoverPage() {
		return coverPage;
	}

	public void setCoverPage(String coverPage) {
		this.coverPage = coverPage;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	@Override
	public String toString() {
		return "Book [title=" + title + ", author=" + author + ", date=" + date + ", language=" + language
				+ ", coverPage=" + coverPage + ", filePath=" + filePath + "]";
	}
	
	public String prettyString() {
		return String.format("[%s] %s, %s (%s, %s)",
				id, title, author, language, date
				);
	}
	
	public static Book fromLink(URL link) {
		Book book = new Book();
		
		try {
			BufferedReader content = new BufferedReader(new InputStreamReader(link.openStream()));
			String lineFeed = "";
			
			// 4 -> found: title, author, date, language
			// messy but most optimal right now
			int completionLevel = 0, criteria = 4;
			while ((lineFeed = content.readLine()) != null && completionLevel < criteria) {
				if (lineFeed.startsWith("Title: ")) {
					book.setTitle(lineFeed);
					criteria++;
				}
				else if (lineFeed.startsWith("Author: ")) {
					book.setAuthor(lineFeed);
					criteria++;
				}
				else if (lineFeed.startsWith("Release date: ")) {
					book.setDate(lineFeed);
					criteria++;
				}
				else if (lineFeed.startsWith("Language: ")) {
					book.setLanguage(lineFeed);
					criteria++;
				}
			}
			
			book.setCoverPage(link.toString().replace(".txt", ".cover.medium.jpg"));
			book.setId(_extractID(link.toString()));
			book.setFilePath(String.format(
					"%s/%s/%s.txt",
					Configuration.PROJECT_ROOT,
					Configuration.projectProperties.getProperty("bookDirectory"),
					book.getId()
					));
			
			// Print text/plain content to file
			// Reset/reopen stream reader
			content.close();
			
			content = new BufferedReader(new InputStreamReader(link.openStream()));
			PrintWriter printWriter = new PrintWriter(new File(book.getFilePath()));
			
			lineFeed = "";
			while ((lineFeed = content.readLine()) != null)
				printWriter.println(lineFeed);
			
			// Close streams
			content.close();
			printWriter.close();
		} catch (IOException exception) {
			logger.severe("Failed to open stream: " + link.toString());
			return null;
		}
		
		book.setId(_extractID(link.toString()));
		
		logger.info(String.format("Parsed book (%s) from url=%s", book.toString(), link.toString()));
		return book;
	}
	
	public HashMap<String, String> toMap() {
		HashMap<String, String> map = new HashMap<>();
		
		map.put("id", id);
		map.put("title", title);
		map.put("author", author);
		map.put("date", date);
		map.put("language", language);
		map.put("coverPage", coverPage);
		map.put("filePath", filePath);
		
		return map;
	}
	
	public static Book fromMap(Map<String, String> map) {
		return new Book(
				map.get("id"),
				map.get("title"),
				map.get("author"),
				map.get("date"),
				map.get("language"),
				map.get("coverPage"),
				map.get("filePath"));
	}
	
	// TODO: Move to an utility class
	private static String _extractID(String link) {
		// 0       1                 2     3    [4]     5
		// https://www.gutenberg.org/cache/epub/24023/pg24023.txt
		String[] tokens = link.replaceAll("//", "/").split("/");
		
		logger.info("Extracted ID: " + tokens[4]);
		return tokens[4];
	}
	
	public static Book fromJson(JSONObject object) 
		throws JSONException {
		return new Book(
				object.getString("id"),
				object.getString("title"),
				object.getString("author"),
				object.getString("date"),
				object.getString("language"),
				object.getString("coverPage"),
				object.getString("filePath")
				);
	}
}
