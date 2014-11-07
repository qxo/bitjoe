package io.tradle.joe.requests;

import io.netty.handler.codec.http.HttpRequest;
import io.tradle.joe.exceptions.InvalidWebHookRequestException;
import io.tradle.joe.protocols.WebHookProtos.Event;
import io.tradle.joe.utils.Utils;

import java.util.Map;

public class WebHookRequest {
	
//	public enum RequestType {
//		Create,
//		Destroy,
//		Clear
//	};

	private final String url;
	private final Event event;
	private final HttpRequest req;
//	private final RequestType type;	
	
	public WebHookRequest(HttpRequest req) {
		Map<String, String> parameters = Utils.getRequestParameters(req);
		this.req = req;
		this.url = parameters.get("url");
		try {
			this.event = Event.valueOf(parameters.get("event"));
		} catch (NullPointerException e) {
			throw new InvalidWebHookRequestException("Missing required parameter 'event'");
		} catch (Exception e) {
			throw new InvalidWebHookRequestException("Invalid value provided for required parameter 'event'");
		}
	}
	
	public String url() {
		return url;
	}
	
	public Event event() {
		return event;
	}

	public HttpRequest httpRequest() {
		return req;
	}
	
//	public RequestType type() {
//		return type;
//	}
}
