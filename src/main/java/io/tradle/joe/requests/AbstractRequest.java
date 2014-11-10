package io.tradle.joe.requests;

import io.netty.handler.codec.http.HttpRequest;
import io.tradle.joe.utils.Utils;

import java.util.Map;

public abstract class AbstractRequest {

	private final HttpRequest httpRequest;
	private final Map<String, String> parameters;

	public AbstractRequest(HttpRequest req) {
		this.httpRequest = req;
		this.parameters = Utils.getRequestParameters(req);
	}
	
	public HttpRequest httpRequest() {
		return httpRequest;
	}
	
	public String param(String name) {
		return parameters.get(name);
	}
	
	public Map<String, String> requestParameters() {
		return parameters;
	}
}
