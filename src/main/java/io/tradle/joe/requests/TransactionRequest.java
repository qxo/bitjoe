package io.tradle.joe.requests;

import io.netty.handler.codec.http.HttpRequest;
import io.tradle.joe.exceptions.InvalidTransactionRequestException;
import io.tradle.joe.sharing.StoragePipe;
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
public class TransactionRequest extends AbstractRequest {

	private final JsonObject json;
	private final String key;
	private final boolean storeInCleartext;
	private List<String> to;

	public TransactionRequest(HttpRequest req) {
		super(req);
		String jsonString = param("data");
		JsonParser parser = new JsonParser();
		try {
			json = (JsonObject) parser.parse(jsonString);
		} catch (JsonSyntaxException j) {
			throw new InvalidTransactionRequestException("request contained malformed json");
		}

		String toCSV = param("to");
		if (toCSV != null) {
			String[] to = toCSV.split(",");
			this.to = ImmutableList.copyOf(to);
		} else
			this.to = ImmutableList.of();

		storeInCleartext = Utils.isTruthy(param("cleartext"));
		this.key = StoragePipe.getStorageKeyStringFor(json.toString());
	}

	public String key() {
		return key;
	}

	public JsonObject data() {
		return json;
	}

	public List<String> to() {
		return to;
	}

	public boolean cleartext() {
		return storeInCleartext;
	}
}
