package io.tradle.joe;

import io.netty.handler.codec.http.QueryStringEncoder;
import io.tradle.joe.utils.TransactionUtils;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Wallet.SendResult;

public class TransactionResponse {

	private final String txUrl;
	private final String txHash;
	private final String keeperQueryUrl;
	private final String keyInStorage;
	
	public TransactionResponse(SendResult result, String keyInStorage) {
		txHash = result.tx.getHashAsString();
		this.keyInStorage = keyInStorage;
		String netInsert = Joe.isOnTestnet() ? "testnet/" : "";
		
//		transactionUrl = "https://blockexplorer.com/" + netInsert + "tx/" + hash;
		txUrl = "https://www.biteasy.com/" + netInsert + "transactions/" + txHash; // faster than blockexplorer.com
		QueryStringEncoder qse = new QueryStringEncoder(Joe.JOE.config().keepers().get(0).toString());
		qse.addParam("key", keyInStorage);
		keeperQueryUrl = qse.toString();
	}
}
