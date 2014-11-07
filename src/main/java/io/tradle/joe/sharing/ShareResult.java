package io.tradle.joe.sharing;

import io.tradle.joe.utils.HttpResponseData;

import org.bitcoinj.core.Wallet.SendResult;

public class ShareResult {

	private final IntermediateFile intermediateFile;
	private final SendResult sendResult;
	
	public ShareResult(SendResult sendResult, IntermediateFile intermediateFile) {
		this.sendResult = sendResult;
		this.intermediateFile = intermediateFile;
	}

	public SendResult sendResult() {
		return sendResult;
	}
	
	public String keyInStorage() {
		return intermediateFile.hash();
	}
	
	public IntermediateFile intermediateFile() {
		return intermediateFile;
	}
}
