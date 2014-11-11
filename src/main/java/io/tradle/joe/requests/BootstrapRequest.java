package io.tradle.joe.requests;

import io.netty.handler.codec.http.HttpRequest;
import io.tradle.joe.Joe;
import io.tradle.joe.events.KeyValue;
import io.tradle.joe.utils.Gsons;
import io.tradle.joe.utils.TransactionRanger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
		
		if (bReq.blockHeightFrom == 0 && bReq.blockHeightTo == 0 && bReq.dateFrom == 0 && bReq.dateTo == 0) {
			bReq.blockHeightFrom = 0;
			bReq.blockHeightTo = Integer.MAX_VALUE;
		}

//		if (this.bReq.blockHeightTo == 0 && this.bReq.dateTo == 0)
//			throw new BadRequestException("Missing required parameters. Please provide 'dateFrom' and 'dateTo' or 'blockHeightFrom' and 'blockHeightTo'");
	}

	public List<KeyValue> execute() {
		TransactionRanger ranger = new TransactionRanger(wallet, Joe.JOE.kit().store());
		if (bReq.blockHeightTo != 0) {
			ranger.fromHeight(bReq.blockHeightFrom)
				  .toHeight(bReq.blockHeightTo);
		}
		else if (bReq.dateTo != 0) {
			ranger.fromDate(bReq.dateFrom)
			  	  .toDate(bReq.dateTo);
		}
		
//		List<KeyValue> data = new ArrayList<KeyValue>();
//		List<Transaction> range = ranger.getRange();
//		for (Transaction t: range) {
//			KeyValue kv = Joe.JOE.receiveData(t);
//			if (kv != null)
//				data.add(kv);
//		}
//		
//		return data;
		return Joe.JOE.receiveData(ranger.getRange());	
	}
	
	class BootstrapRequestParams {
		private int blockHeightFrom;
		private int blockHeightTo;
		private int dateFrom;
		private int dateTo;
	}
}
