package net.etfbl.model.api;

import org.json.JSONObject;

public class RequestResponse {
	private String input;
	private String message;
	
	public RequestResponse(String input, String message) {
		super();
		this.input = input;
		this.message = message;
	}
	
	public RequestResponse() {
		super();
	}

	public String getInput() {
		return input;
	}

	public void setInput(String input) {
		this.input = input;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	public static RequestResponse fromJsonObject(JSONObject jsonObject) {
		return new RequestResponse(
				jsonObject.getString("input"),
				jsonObject.getString("message")
				);
	}
}
