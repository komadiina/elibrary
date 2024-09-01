package net.etfbl.test;

import java.io.*;
import java.util.*;

import net.etfbl.config.Configuration;
import net.etfbl.model.*;
import net.etfbl.model.serial.*;

public class UsersTest {
	public static void main(String[] args) 
		throws IOException, ClassCastException {
		Properties props = Configuration.loadProperties("project");
		List<User> users = new ArrayList<>();
		users.addAll(Arrays.asList(
//			new User("ogg1", "Ognjen", "Komadina", "komadina.ognjen@gmail.com", true),
//			new User("ogg2", "Ognjen", "Komadina", "komadina.ognjen2n@gmail.com", true),
//			new User("ogg3", "Ognjen", "Komadina", "komadina.ognjen3@gmail.com", false),
//			new User("ogg4", "Ognjen", "Komadina", "komadina.ognjen4@gmail.com", false)
		));
		XMLSerializer<List<User>> s = new XMLSerializer<>();
		String path = String.format("%s/%s", Configuration.PROJECT_ROOT, props.getProperty("demoUsersStore"));
		System.out.println(path);
		s.serialize(users, path);
		
		List<User> read = s.deserialize(path);
		System.out.println(read);
	}
}
