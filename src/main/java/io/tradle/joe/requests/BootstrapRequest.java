package io.tradle.joe.requests;

import io.netty.handler.codec.http.HttpRequest;
import io.tradle.joe.Joe;
import io.tradle.joe.events.KeyValue;
import io.tradle.joe.exceptions.BadRequestException;
import io.tradle.joe.extensions.WebHooksExtension;
import io.tradle.joe.utils.Gsons;
import io.tradle.joe.utils.TransactionCursor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

public class BootstrapRequest extends AbstractRequest {

	private BootstrapRequestParams bReq;
	private Wallet wallet;

	public BootstrapRequest(HttpRequest req) {
		super(req);
		this.wallet = Joe.JOE.wallet();

		JsonElement json = Gsons.def().toJsonTree(requestParameters(), new TypeToken<Map<String, String>>() {}.getType());
		bReq = Gsons.def().fromJson(json, BootstrapRequestParams.class);
		
		if (bReq.blockHeightTo != 0) 
			throw new UnsupportedOperationException(); // TODO: implement

		if (this.bReq.blockHeightTo == 0 && this.bReq.dateTo == 0)
			throw new BadRequestException("Missing required parameters. Please provide 'dateFrom' and 'dateTo' or 'blockHeightFrom' and 'blockHeightTo'");
	}

	public List<KeyValue> execute() {
		TransactionCursor cursor = new TransactionCursor(wallet, Joe.JOE.kit().store());
		if (bReq.blockHeightTo != 0) {
			cursor.fromHeight(bReq.blockHeightFrom)
				  .toHeight(bReq.blockHeightTo);
		}
		else if (bReq.dateTo != 0) {
			cursor.fromDate(bReq.dateFrom)
			  	  .toDate(bReq.dateTo);
		}
		
		List<KeyValue> data = new ArrayList<KeyValue>();
		cursor.forEach(new Consumer<Transaction>() {
			@Override
			public void accept(Transaction t) {
				KeyValue keyValue = Joe.JOE.receiveData(t);
				if (keyValue != null)
					data.add(keyValue);
			}
		});
	
		return data;
	}
	
	class BootstrapRequestParams {
		private int blockHeightFrom;
		private int blockHeightTo;
		private int dateFrom;
		private int dateTo;
	}
}
