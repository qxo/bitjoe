package io.tradle.joe;

import io.netty.handler.codec.http.QueryStringEncoder;
import io.tradle.joe.utils.TransactionUtils;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Wallet.SendResult;

public class TransactionResponse {

	private final String transactionUrl;
	private final String hash;
	private final String keeperQueryUrl;
	private final String data;
	
	public TransactionResponse(SendResult result, String data) {
		hash = result.tx.getHashAsString();
		this.data = data;
		transactionUrl = "https://www.biteasy.com/testnet/transactions/" + hash;
		QueryStringEncoder qse = new QueryStringEncoder(Joe.JOE.config().keepers().get(0).toString());
		qse.addParam("key", data);
		keeperQueryUrl = qse.toString();
	}
}
