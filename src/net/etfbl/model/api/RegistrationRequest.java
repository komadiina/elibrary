package net.etfbl.model.api;

import java.io.Serializable;
import java.util.Objects;

import net.etfbl.model.Role;

public class RegistrationRequest implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String username;
	private String password1;
	private String password2;
	private String firstName;
	private String lastName;
	private String email;
	
	private Role role = Role.GUEST;
	private boolean activated = false;
	

	public RegistrationRequest() {
		super();
	}

	public RegistrationRequest(String username, String password1, String password2, String firstName, String lastName,
			String email) {
		super();
		this.username = username;
		this.password1 = password1;
		this.password2 = password2;
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
	}



	public String getPassword1() {
		return password1;
	}

	public void setPassword1(String password1) {
		this.password1 = password1;
	}

	public String getPassword2() {
		return password2;
	}

	public void setPassword2(String password2) {
		this.password2 = password2;
	}

	@Override
	public int hashCode() {
		return Objects.hash(activated, email, firstName, lastName, username);
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

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}
	
	public String toJson() {
		return String.format("{"
				+ "\"username\": \"%s\","
				+ "\"email\": \"%s\","
				+ "\"firstName\": \"%s\","
				+ "\"lastName\": \"%s\","
				+ "\"password1\": \"%s\","
				+ "\"password2\": \"%s\""
				+ "}",
				username, email, firstName, lastName, password1, password2
				);
	}
}
