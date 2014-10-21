package io.tradle.joe;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.util.List;
import java.util.Map;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.wallet.KeyChain.KeyPurpose;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TransactionRequest {

	private final JsonObject json;
	private final HttpRequest req;
//	private List<String> owners;
	private ECKey toKey;

	public TransactionRequest(HttpRequest req) {
		this.req = req;
		
		QueryStringDecoder qs = new QueryStringDecoder(req.getUri());
		Map<String, List<String>> parameters = qs.parameters();
		String jsonString = parameters.get("data").get(0);
//		toAddress = parameters.get("to").get(0);
		
		toKey = Joe.JOE.wallet().currentKey(KeyPurpose.RECEIVE_FUNDS);
		
    	JsonParser parser = new JsonParser();
    	json = (JsonObject) parser.parse(jsonString);
    		
//    	JsonArray jOwners = json.getAsJsonArray("owners");
//    	List<String> owners = new ArrayList<String>(jOwners.size());
//    	for (JsonElement j: jOwners) {
//    		owners.add(j.getAsString());
//    	}
//    	
//    	this.owners = Collections.unmodifiableList(owners); 
	}
	
//	public List<String> owners() {
//		return owners;
//	}
	
	public JsonObject data() {
		return json;
	}

	public HttpRequest httpRequest() {
		return req;
	}
	
	public ECKey getDestinationKey() {
		return toKey;
	}
}
