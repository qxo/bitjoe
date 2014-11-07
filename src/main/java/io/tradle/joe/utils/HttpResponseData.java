package io.tradle.joe.utils;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

public class HttpResponseData {
	private int code;
	private String response;
	private String json;
	
	public HttpResponseData(int code, String response) {
		this.code = code;
		this.response = response;
	}
	
	public HttpResponseData(int code, JsonElement json) {
		this.code = code;
		this.json = this.response = json.toString();
	}
	
	public int code() {
		return code;
	}
	
	public String response() {
		return response;
	}

	public String toJsonString() {
		return toJsonString(false);
	}

	public String toJsonString(boolean prettyPrint) {
		if (json == null) {
			GsonBuilder builder = new GsonBuilder();
			if (prettyPrint)
				builder.setPrettyPrinting();
			
			json = builder.create().toJson(this);
		}
		
		return json;
	}
}
