package net.etfbl.middleware;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import net.etfbl.config.Configuration;
import net.etfbl.config.IOLogger;
import net.etfbl.model.*;
import redis.clients.jedis.*;

public class BooksMiddleware {
	private static final IOLogger logger = new IOLogger(BooksMiddleware.class.getName());
	
	private static BooksMiddleware instance = null;
	
	public List<Book> books;
	public List<String> bookIDs = new ArrayList<String>();
	public HashMap<Book, Integer> shelf = new HashMap<Book, Integer>();
	
	public List<BookProposal> proposals = new ArrayList<BookProposal>();
	public List<String> proposalIds = new ArrayList<String>();
	
	private static final String REDIS_INSTANCE_NAME = "books";
	private JedisPool pool = null;
	private Jedis jedis = null;
	
	public BooksMiddleware() {
		books = _loadFromFS();
		
		openPool();
		jedis = getPoolInstance();
		_storeIntoJedis(books);
	}
	
	public static BooksMiddleware getInstance() {
		if (instance == null)
			instance = new BooksMiddleware();
		
		return instance;
	}
	
	@SuppressWarnings("deprecation")
 	private List<Book> _loadFromFS() {
		// Load all links
		String linksPath = String.format("%s/%s", 
				Configuration.PROJECT_ROOT,
				Configuration.projectProperties.getProperty("bookLinks")
				);
		
		List<String> links = new ArrayList<String>();
		try {
			links = Files.readAllLines(Path.of(linksPath));
		} catch (IOException exception) {
			logger.warning("Failed to load " + linksPath);
		}
		
		List<Book> parsed = new ArrayList<Book>();
		for (String link : links)
			try {
				logger.info(String.format("Parsing book from link: %s", link));
				Book result = Book.fromLink(new URL(link));
				parsed.add(result);
				logger.info("Successfully parsed book, path: " + parsed.getLast().getFilePath());
			} catch (MalformedURLException exception) {
				logger.warning(String.format("Invalid URL (%s), skipping...", link));
				continue;
			}
		
		logger.info("Finished initalizing books from " + linksPath);
		return parsed;
	}
	
	private void _storeIntoJedis(List<Book> books) {
		for (Book book : books)
			add(book);
	}
	
	
	public void openPool() {
		pool = new JedisPool("redis://localhost:6379");
	}
	
	public void closePool() {
		pool.close();
	}
	
	public Jedis getPoolInstance() {
		if (jedis == null)
		{
			if (pool.isClosed()) 
				openPool();
			
			jedis = pool.getResource();
			jedis.set(REDIS_INSTANCE_NAME, "OK");
		}
		
		return jedis;
	}
	
	// CRUD methods access objects via a REDIS instance
	public List<Book> getAll() {
		List<Book> result = new ArrayList<Book>();
		
		for (String id : bookIDs) {
			logger.info("Retrieving book from REDIS, id=" + id);
			
			result.add(Book.fromMap(jedis.hgetAll(String.format(
					"%s:books:map:%s",
					REDIS_INSTANCE_NAME,
					id
					))));
		}
			
		return result;
	}
	
	public Book get(String id) {
		if (!bookIDs.contains(id))
			return null;
		
		Book result = Book.fromMap(jedis.hgetAll(String.format(
				"%s:books:map:%s",
				REDIS_INSTANCE_NAME,
				id
				)));
		return result;
	}
	
	public boolean add(Book book) {
		jedis.hmset(
				String.format("%s:books:map:%s", REDIS_INSTANCE_NAME, book.getId()), 
				book.toMap());
		bookIDs.add(book.getId());
		return true;
	}
	
	public boolean update(Book book, String id) {
		String jedisPath = String.format("%s:books:map:%s", REDIS_INSTANCE_NAME, id);
		Book result = Book.fromMap(
				jedis.hgetAll(jedisPath));
		
		if (result != null) {
			// TODO: problem occurs when the id is changed
			// Remove from REDIS
			jedis.lpop(jedisPath);
			
			// Add back into REDIS
			jedis.hmset(jedisPath, book.toMap());
			
			return true;
		}
		
		return false;
	}
	
	public boolean delete(Book book) {
		jedis.del(String.format("%s:books:map:%s", REDIS_INSTANCE_NAME, book.getId()));
		return true;
	}
	
	public String addFromPG(String id) {
		String url = String.format("https://www.gutenberg.org/cache/epub/%s/pg%s.txt", id, id);
		try {
			add(Book.fromLink(new URL(url)));
		} catch (MalformedURLException ex) {
			logger.warning(String.format("Failed to parse URL: %s, not adding.", url));
			return null;
		}
		return url;
	}
	
	public void submitProposal(BookProposal proposal) {
		proposals.add(proposal);
		try {
			Utility.sendMulticastMessage("New proposal submitted: " + proposal.toString());
		} catch (IOException exception) {
			logger.severe("Failed to send multicast message: " + exception.getMessage());
		}
	}
	
	public void reviewProposal(int proposalIndex, boolean approved) {
        if (proposalIndex >= 0 && proposalIndex < proposals.size()) {
            BookProposal proposal = proposals.get(proposalIndex);
            proposal.setApproved(approved);
            String message = approved ? "Proposal approved: " : "Proposal rejected: ";
            message += proposal.toString();
            try {
                Utility.sendMulticastMessage(message);
            } catch (IOException e) {
                logger.severe("Failed to send multicast message: " + e.getMessage());
            }
        }
    }
}
