package io.tradle.joe.utils;

import com.google.gson.GsonBuilder;

public class HttpResponseData {
	int code;
	String response;
	
	public HttpResponseData(int code, String response) {
		this.code = code;
		this.response = response;
	}
	
	public int code() {
		return code;
	}
	
	public String response() {
		return response;
	}

	public String toJsonString() {
		return new GsonBuilder().create().toJson(this);
	}
}
