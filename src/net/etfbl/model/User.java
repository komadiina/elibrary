package net.etfbl.model;

import java.io.Serializable;
import java.util.Objects;

import org.json.JSONException;
import org.json.JSONObject;

import net.etfbl.model.api.RegistrationRequest;

public class User implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 11L;
	
	private String username;
	private String password;
	private String firstName;
	private String lastName;
	private String email;
	
	private Role role;
	private boolean activated;

	public User(String username, String password, String firstName, String lastName, String email) {
		super();
		this.username = username;
		this.password = password;
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
	}

	public User(String username, String password, String firstName, String lastName, String email, Role role) {
		super();
		this.username = username;
		this.password = password;
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.role = role;
	}

	public User(String username, String password, String firstName, String lastName, String email, Role role,
			boolean activated) {
		super();
		this.username = username;
		this.password = password;
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.role = role;
		this.activated = activated;
	}
	
	
	public User() {}

	@Override
	public int hashCode() {
		return Objects.hash(activated, email, firstName, lastName, username);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		User other = (User) obj;
		return activated == other.activated && Objects.equals(email, other.email)
				&& Objects.equals(firstName, other.firstName) && Objects.equals(lastName, other.lastName)
				&& Objects.equals(username, other.username);
	}

	@Override
	public String toString() {
		return "User [username=" + username + ", firstName=" + firstName + ", lastName=" + lastName + ", email=" + email
				+ ", activated=" + activated + "]";
	}

	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getFirstName() {
		return firstName;
	}
	
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	
	public String getLastName() {
		return lastName;
	}
	
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	
	public String getEmail() {
		return email;
	}
	
	public void setEmail(String email) {
		this.email = email;
	}
	
	public boolean getActivated() {
		return activated;
	}
	
	public void setActivated(boolean activated) {
		this.activated = activated;
	}
	
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}
	
	public static User fromRegistrationRequest(RegistrationRequest request) {
		return new User(
					request.getUsername(),
					request.getPassword1(),
					request.getFirstName(),
					request.getLastName(),
					request.getEmail()
				);
	}
	
	public String toJson() {
		return String.format("{\"username\": \"%s\", \"password\": \"%s\", \"firstName\": \"%s\", \"lastName\": \"%s\", \"email\": \"%s\", \"role\": \"%s\", \"activated\": %b}",
					username, password, firstName, lastName, email, role.toString(), activated
				);
	}

	private static Role getRole(String input) {
		if (input.equals("LIBRARIAN"))
			return Role.LIBRARIAN;
		else if (input.equals("GUEST"))
			return Role.GUEST;
		else if (input.equals("SUPPLIER"))
			return Role.SUPPLIER;
		
		return Role.MEMBER;
	}
	
	public static User fromJsonObject(JSONObject json) 
		throws JSONException {
		return new User(
				json.getString("username"),
				json.getString("password"),
				json.getString("firstName"),
				json.getString("lastName"),
				json.getString("email"),
				getRole(json.getString("role")),
				(boolean)json.get("activated")
				);
	}
}
