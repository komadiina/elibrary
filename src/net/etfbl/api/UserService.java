package net.etfbl.api;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;

import net.etfbl.middleware.SessionManager;
import net.etfbl.middleware.UsersMiddleware;
import net.etfbl.model.*;
import net.etfbl.model.api.LoginRequest;
import net.etfbl.model.api.RegistrationRequest;
import net.etfbl.model.api.RequestResponse;

@Path("/users")
public class UserService {
	public static final String API_HOST = "http://localhost:8080/mdp/api/users";
	
	private UsersMiddleware middleware;
	private static SessionManager sessionManager;
	
	public UserService() {
		middleware = UsersMiddleware.getInstance(false);
		sessionManager = SessionManager.getInstance();
	}
	
	private boolean isAuthorized(String authHeader) {
		if (!sessionManager.isTokenValid(authHeader.split(" ")[1]))
			return false;
		
		return true;
	}
	
	private Response unauthorized() {
		return Response
				.status(Status.UNAUTHORIZED)
				.entity(new RequestResponse(
							"not-authorized", 
							"Unauthorized, please log in and refresh your authentication token."))
				.build();
	}
	
	@GET
	@Path("/all")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAll(@HeaderParam("Authorization") String authHeader) {
		if (authHeader == null || !isAuthorized(authHeader))
			return unauthorized();
		
		return Response
				.status(Status.OK)
				.entity(middleware.getAll())
				.build();
	}
	
	@GET
	@Path("/find")
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@QueryParam("email") String email, @HeaderParam("Authorization") String authHeader) {
		if (authHeader == null || !isAuthorized(authHeader))
			return unauthorized();
		
		// Remove invalid characters
		email = email.replaceAll("\\?\\!", "");
		
		User result = middleware.get(email);
		if (result == null)
			return Response
					.status(Status.NOT_FOUND)
					.entity(new RequestResponse(email, "No such user found."))
					.build();
		
		return Response
				.status(Status.OK)
				.entity(result)
				.build();
	}
	
	@POST
	@Path("/register")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response register(RegistrationRequest request) {
		if (!request.getPassword1().equals(request.getPassword2()))
			return Response
					.status(Status.BAD_REQUEST)
					.entity(new RequestResponse(
								String.format("%s != %s", request.getPassword1(), request.getPassword2()), 
								"Password mismatch."))
					.build();
		
		User user = User.fromRegistrationRequest(request);
		if (middleware.get(request.getEmail()) != null
				|| middleware.getUsername(request.getUsername()) != null) 
			return Response
					.status(Status.BAD_REQUEST)
					.entity(new RequestResponse(
								request.getUsername() 
								+ ":" 		
								+ request.getEmail(), 
								"User already exists."))
					.build();
	
		// Success, set uninitialized fields
		user.setRole(Role.GUEST);
		user.setActivated(false);
		if (middleware.register(user))
			return Response
					.status(Status.OK)
					.entity(user)
					.build();
		
		// Unable to register
		return Response
				.status(Status.BAD_REQUEST)
				.entity(new RequestResponse(user.toString(), "User could not be registered."))
				.build();
	}
	
	@POST
	@Path("/promote")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response promote(User manager, @QueryParam("email") String userEmail, @QueryParam("role") String role) {
		User user = middleware.get(userEmail);
		if (user == null)
			return Response
					.status(Status.NOT_FOUND)
					.entity(new RequestResponse(
								userEmail,
								"No such user found."
							))
					.build();
		
		if (!manager.getRole().equals(Role.LIBRARIAN))
			return Response
					.status(Status.BAD_REQUEST)
					.entity(new RequestResponse(
								manager.toString() + user.toString(), 
								"Received manager does not have the needed privilege."))
					.build();
		
		if (role.equals("SUPPLIER"))
			user.setRole(Role.SUPPLIER);
		else if (role.equals("LIBRARIAN"))
			user.setRole(Role.LIBRARIAN);
		else user.setRole(Role.MEMBER);
		middleware.update(user, userEmail);
		
		return Response
				.status(Status.OK)
				.entity(user)
				.build();
	}
	
	@POST
	@Path("/activate")
	@Consumes(MediaType.APPLICATION_JSON) 
	@Produces(MediaType.APPLICATION_JSON)
	public Response activate(User manager, @QueryParam("email") String userEmail) {
		User user = middleware.get(userEmail);
		
		if (user == null)
			return Response
					.status(Status.NOT_FOUND)
					.entity(new RequestResponse(
								userEmail,
								"No such user found."
							))
					.build();
		
		if (!manager.getRole().equals(Role.LIBRARIAN))
			return Response
					.status(Status.BAD_REQUEST)
					.entity(new RequestResponse(
								manager.toString() + user.toString(), 
								"Received manager does not have the needed privilege."))
					.build();
		
		user.setRole(Role.MEMBER);
		middleware.update(user, user.getEmail());
		middleware.activateAccount(user);
		
		System.out.println(middleware.users);
		
		return Response
				.status(Status.OK)
				.entity(user)
				.build();
	}
	
	@POST
	@Path("/deactivate")
	@Consumes(MediaType.APPLICATION_JSON) 
	@Produces(MediaType.APPLICATION_JSON)
	public Response deactivate(User manager, @QueryParam("email") String userEmail) {
		User user = middleware.get(userEmail);
		
		if (user == null)
			return Response
					.status(Status.NOT_FOUND)
					.entity(new RequestResponse(
								userEmail,
								"No such user found."
							))
					.build();
		
		if (!manager.getRole().equals(Role.LIBRARIAN))
			return Response
					.status(Status.BAD_REQUEST)
					.entity(new RequestResponse(
								manager.toString() + user.toString(), 
								"Received manager does not have the needed privilege."))
					.build();
		
		user.setRole(Role.GUEST);
		middleware.update(user, user.getEmail());
		middleware.deactivateAccount(user);
		
		return Response
				.status(Status.OK)
				.entity(user)
				.build();
	}
	
	@POST
	@Path("/login")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response login(LoginRequest credentials) {
		User result = middleware.getUsername(credentials.getUsername());
		
		if (result != null) {
			if (result.getActivated() == false) 
				return Response
						.status(Status.UNAUTHORIZED)
						.entity(new RequestResponse(result.toString(), "Account not activated."))
						.build();
			
			if (credentials.getPassword().equals(result.getPassword())) {
				String authToken = sessionManager.generateToken(credentials.getUsername());
					
				System.out.printf("Logged in user %s:%s\n", credentials.getUsername(), credentials.getPassword());
				
				return Response
						.status(Status.OK)
						.entity(String.format("{\"authToken\": \"%s\", \"user\": %s}", authToken, result.toJson()))
						.build();
			}
			else return Response
					.status(Status.NOT_ACCEPTABLE)
					.entity(new RequestResponse(credentials.toString(), "Invalid credentials."))
					.build();
		}
		
		return Response
				.status(Status.BAD_REQUEST)
				.entity(new RequestResponse(credentials.toString(), "No such users exists."))
				.build();
	}
	
	@POST
	@Path("/logout")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response logout(@HeaderParam("Authorization") String authHeader, User user) {
		String token = authHeader.split(" ")[1];
		
		// verify token credibility
		if (!sessionManager.getToken(user.getUsername()).equals(token))
			return Response
					.status(Status.FORBIDDEN)
					.entity(new RequestResponse(authHeader, "Non-matching authToken received."))
					.build();
		
		// invalidate token (delete)		
		sessionManager.invalidateToken(user.getUsername());
		return Response
				.status(Status.OK)
				.entity(new RequestResponse(user.toString(), "Invalidated session and logged out successfully."))
				.build();
	}
	
	@POST
	@Path("/remove")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response remove(User manager, @QueryParam("email") String email, @HeaderParam("Authorization") String authHeader) {
		String token = authHeader.split(" ")[1];
		
		// verify token credibility
		if (!sessionManager.getToken(manager.getUsername()).equals(token))
			return Response
					.status(Status.FORBIDDEN)
					.entity(new RequestResponse(authHeader, "Non-matching authToken received."))
					.build();
		
		User result = middleware.get(email);
		
		if (result == null)
			return Response
					.status(Status.BAD_REQUEST)
					.entity(new RequestResponse(
							email,
							"User not found."
							))
					.build();
		
		middleware.delete(result);
		
		return Response
				.status(Status.OK)
				.entity(new RequestResponse(
						email,
						"User removed successfully"
						))
				.build();
	}
}




















