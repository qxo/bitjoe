package io.tradle.joe.requests;

import io.netty.handler.codec.http.HttpRequest;
import io.tradle.joe.exceptions.InvalidWebHookRequestException;
import io.tradle.joe.protocols.WebHookProtos.Event;
import io.tradle.joe.utils.Utils;

import java.util.Map;

public class WebHookRequest extends AbstractRequest {
	
	private final String url;
	private final Event event;
	
	public WebHookRequest(HttpRequest req) {
		super(req);
		this.url = param("url");
		try {
			this.event = Event.valueOf(param("event"));
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
}
