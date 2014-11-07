package io.tradle.joe.requests;

import io.netty.handler.codec.http.HttpRequest;
import io.tradle.joe.exceptions.InvalidTransactionRequestException;
import io.tradle.joe.utils.Utils;

import java.util.List;
import java.util.Map;

import org.bitcoinj.core.ECKey;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * Parses the transaction request
 */
public class TransactionRequest {

	private final JsonObject json;
	private final HttpRequest req;
	private final boolean storeInCleartext;
	private List<String> to;
	private Map<String, String> parameters;

	public TransactionRequest(HttpRequest req) {
		this.req = req;		
		
		parameters = Utils.getRequestParameters(req);
		String jsonString = parameters.get("data");
    	JsonParser parser = new JsonParser();
    	try {
    		json = (JsonObject) parser.parse(jsonString);
    	} catch (JsonSyntaxException j) {
    		throw new InvalidTransactionRequestException("request contained malformed json");
    	}

    	String toCSV = parameters.get("to");
    	if (toCSV != null) {
			String[] to = toCSV.split(",");
			this.to = ImmutableList.copyOf(to);
    	}
    	else
    		this.to = ImmutableList.of();
    	
    	storeInCleartext = Utils.isTruthy(parameters.get("cleartext"));
	}
	
	public JsonObject data() {
		return json;
	}

	public HttpRequest httpRequest() {
		return req;
	}
	
	public List<String> to() {
		return to;
	}
	
	public String param(String name) {
		return parameters.get(name);
	}

	public boolean cleartext() {
		return storeInCleartext;
	}
}
