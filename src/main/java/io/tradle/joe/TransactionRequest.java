package io.tradle.joe;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.tradle.joe.exceptions.InvalidTransactionRequestException;
import io.tradle.joe.utils.Utils;

import java.util.List;
import java.util.Map;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.wallet.KeyChain.KeyPurpose;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * Parses the transaction request
 */
public class TransactionRequest {

	private final JsonObject json;
	private final HttpRequest req;
//	private List<String> owners;
	private ECKey toKey;
	private Map<String, String> parameters;

	public TransactionRequest(HttpRequest req) {
		this.req = req;
		
//		QueryStringDecoder qs = new QueryStringDecoder(req.getUri());
		parameters = Utils.getRequestParameters(req);
		String jsonString = parameters.get("data");
//		toAddress = parameters.get("to").get(0);

		// TODO: send to actual recipients instead of self
		toKey = Joe.JOE.wallet().currentKey(KeyPurpose.RECEIVE_FUNDS);
		
    	JsonParser parser = new JsonParser();
    	try {
    		json = (JsonObject) parser.parse(jsonString);
    	} catch (JsonSyntaxException j) {
    		throw new InvalidTransactionRequestException("request contained malformed json");
    	}
	}
	
	public JsonObject data() {
		return json;
	}

	public HttpRequest httpRequest() {
		return req;
	}
	
	public ECKey getDestinationKey() {
		return toKey;
	}
	
	public String param(String name) {
		return parameters.get(name);
	}
}
