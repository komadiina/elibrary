package net.etfbl.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;

import net.etfbl.config.IOLogger;
import net.etfbl.middleware.BooksMiddleware;
import net.etfbl.middleware.SessionManager;
import net.etfbl.middleware.UsersMiddleware;
import net.etfbl.middleware.Utility;
import net.etfbl.model.Book;
import net.etfbl.model.User;
import net.etfbl.model.api.DownloadRequest;
import net.etfbl.model.api.RequestResponse;

@Path("/books")
public class BookService {
	private static final IOLogger logger = new IOLogger(BookService.class.getName());
	public static final String API_HOST = "http://localhost:8080/mdp/api/books";
	
	private BooksMiddleware middleware;
	private static SessionManager sessionManager = SessionManager.getInstance();
	
	public BookService() {
		middleware = BooksMiddleware.getInstance();
	}
	
	@GET
	@Path("/all")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAll(@HeaderParam("Authorization") String authHeader) {
		return Response
				.status(Status.OK)
				.entity(middleware.getAll())
				.build();
	}
	
	@GET
	@Path("/find")
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@QueryParam("id") String id) {
		Book result = middleware.get(id);
		
		if (result != null)
			return Response
					.status(Status.OK)
					.entity(result)
					.build();
		
		return Response
				.status(Status.NOT_FOUND)
				.entity(new RequestResponse(id, "No book with matching id exists."))
				.build();
	}
	
	@POST
	@Path("/download")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response download(DownloadRequest request) {
		Book book = middleware.get(request.getBookID());
		
		if (book == null) 
			return Response
					.status(Status.NOT_FOUND)
					.build();
		
		User user = UsersMiddleware.getInstance(false).get(request.getMailTo());
		
//		// Zip file
//		String zipArchivePath = book.getFilePath() + ".zip";
//		try {
//			ZipOutputStream out = new ZipOutputStream(new FileOutputStream(new File(zipArchivePath)));
//			ZipEntry entry = new ZipEntry(book.getFilePath());
//			out.putNextEntry(entry);
//			
//			StringBuilder sb = new StringBuilder();
//			sb.append("Downloaded from e-Library.");
//			
//			byte[] data = sb.toString().getBytes();
//			out.write(data, 0, data.length);
//			out.closeEntry();
//			out.close();
//		} catch (IOException exception) {
//			logger.severe("Could not ZIP book: " + book.getFilePath());
//			return Response
//					.status(Status.INTERNAL_SERVER_ERROR)
//					.build();
//		}
		String zipArchivePath = book.getFilePath() + ".zip";
		try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(new File(zipArchivePath)));
		     FileInputStream fis = new FileInputStream(new File(book.getFilePath()))) {
		     
		    // Create a new ZIP entry for the book file
		    ZipEntry entry = new ZipEntry(new File(book.getFilePath()).getName());
		    out.putNextEntry(entry);

		    // Write "Downloaded from e-Library." at the beginning of the file
		    StringBuilder sb = new StringBuilder();
		    sb.append("Downloaded from e-Library.\n\n");
		    byte[] headerData = sb.toString().getBytes();
		    out.write(headerData, 0, headerData.length);

		    // Write the actual contents of the book file
		    byte[] buffer = new byte[1024];
		    int length;
		    while ((length = fis.read(buffer)) > 0) {
		        out.write(buffer, 0, length);
		    }

		    out.closeEntry();
		    
		} catch (IOException exception) {
		    logger.severe("Could not ZIP book: " + book.getFilePath());
		    return Response
		            .status(Status.INTERNAL_SERVER_ERROR)
		            .build();
		}
		
		Utility.sendMailWithAttachment(
				user.getEmail(), 
				String.format("[e-Library] Download request for %s", book.getTitle()),
				String.format("Your book content for %s is available below.", book.prettyString()), 
				zipArchivePath);
		
		return Response.status(200).build();
	}
}
