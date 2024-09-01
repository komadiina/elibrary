package net.etfbl.middleware;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


import com.google.gson.*;
import com.sendgrid.*;
import com.sendgrid.helpers.mail.*;
import com.sendgrid.helpers.mail.objects.*;

import net.etfbl.config.Configuration;
import net.etfbl.config.IOLogger;
import net.etfbl.model.Book;
import net.etfbl.model.api.LoginRequest;

public class Utility {
	private static final IOLogger logger = new IOLogger(Utility.class.getName());
	
	public static final String PROPOSAL_MULTICAST_GROUP = "230.0.0.0";
	public static final int PORT = 8446;
	
	public static String getToken(LoginRequest request) {
		return Base64.getEncoder().encode(
					String.format("%s:%s", request.getUsername(), request.getPassword()).getBytes()
				).toString();
	}
	
	public static boolean isAuthorized(String authHeader) {
		return (SessionManager.getInstance()).isTokenValid((authHeader.split(" ")[1]));
	}
	
	public static String processRequest(String endpoint, String method, Map<String, String> headers, String body) throws Exception {
        String result = "";
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        
        System.out.println(body);
        
        try {
            URL url = new URL(endpoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Content-Type", "application/json");

            if (headers != null)
	            for (Map.Entry<String, String> headerProp : headers.entrySet())
	            	connection.setRequestProperty(headerProp.getKey(), headerProp.getValue());
            
            // POST/PUT - doOutput
            if ("POST".equals(method) || "PUT".equals(method)) {
                connection.setDoOutput(true);
                
                OutputStream os = connection.getOutputStream();
                os.write(body.toString().getBytes());
                os.flush();
            }

            // Get the response
            int status = connection.getResponseCode();
            logger.info(endpoint + " - " + status);
            
            if (status >= 200 && status < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } else {
                logger.warning(String.format("Bad response (%s), status code: %d", endpoint, status));
            }

            // Read the response
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            
            result = response.toString();
        } catch (JsonSyntaxException e) {
            logger.severe(String.format("Failed to parse JSON response, endpoint=%s", endpoint));
            return null;
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        
        return result;
    }
	
	public static void sendMulticastMessage(String message)
		throws IOException {
		try (MulticastSocket ms = new MulticastSocket()) {
			InetAddress group = InetAddress.getByName(PROPOSAL_MULTICAST_GROUP);
			byte[] buffer = message.getBytes();
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
			ms.send(packet);
		}
	}
	
	public static void sendMailWithAttachment(String to, String subject, String body, String attachmentPath) {
		Email mailFrom = new Email("komadina.ognjen@gmail.com");
		Email mailTo = new Email(to);
		Content content = new Content("text/plain", body);
		Mail mail = new Mail(mailFrom, subject, mailTo, content);
		
		Attachments attachments = new Attachments();
		attachments.setFilename("book.zip");
		attachments.setType("application/zip");
		attachments.setDisposition("attachment");
		
		try {
			byte[] attachmentContentBytes = Files.readAllBytes(Paths.get(attachmentPath));
			String attachmentContent = Base64.getMimeEncoder().encodeToString(attachmentContentBytes);
			attachments.setContent(attachmentContent);
			mail.addAttachments(attachments);
			
		} catch (IOException exception) {
			logger.severe("Unable to provide attachment for mail - could not read contents for file " + attachmentPath);
			return;
		}
		
		try {
			SendGrid sg = new SendGrid(Configuration.projectProperties.getProperty("sendgridAPIKey"));
			Request request = new Request();
			
			request.setMethod(Method.POST);
			request.setEndpoint("mail/send");
			request.setBody(mail.build());
			
			Response response = sg.api(request);
		} catch (IOException exception) {
			logger.severe("Failed to send mail: " + exception.getMessage());			
		}
	}
	
	@SuppressWarnings("deprecation")
	public static Book fromID(String id) 
		throws MalformedURLException {
		return Book.fromLink(
				new URL(String.format("https://www.gutenberg.org/cache/epub/%s/pg%s.txt", id, id)));
	}
}
