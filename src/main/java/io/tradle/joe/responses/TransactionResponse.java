package io.tradle.joe.responses;

import io.netty.handler.codec.http.QueryStringEncoder;
import io.tradle.joe.Joe;

import org.bitcoinj.core.Wallet.SendResult;

public class TransactionResponse {

	private String txUrl;
	private String txHash;
	private String keeperQueryUrl;
	private String keyInStorage;
	
	public TransactionResponse(SendResult result, String keyInStorage) {
		this.keyInStorage = keyInStorage;
		if (result != null) {
			txHash = result.tx.getHashAsString();
			txUrl = "http://tbtc.blockr.io/tx/info/" + txHash;
		}
		
		QueryStringEncoder qse = new QueryStringEncoder(Joe.JOE.config().keepers().get(0).toString());
		qse.addParam("key", keyInStorage);
		keeperQueryUrl = qse.toString();
	}
	
	public String txUrl() {
		return txUrl;
	}
	
	public String txHash() {
		return txHash;
	}
	
	public String keeperQueryUrl() {
		return keeperQueryUrl;
	}

	public String keyInStorage() {
		return keyInStorage;
	}

}
