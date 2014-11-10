package io.tradle.joe.sharing;

import io.tradle.joe.utils.HttpResponseData;

import org.bitcoinj.core.Wallet.SendResult;

public class Permission {

	private final PermissionFile intermediateFile;
	private final SendResult sendResult;
	
	public Permission(SendResult sendResult, PermissionFile intermediateFile) {
		this.sendResult = sendResult;
		this.intermediateFile = intermediateFile;
	}

	public SendResult sendResult() {
		return sendResult;
	}
	
	public String keyInStorage() {
		return intermediateFile.hash();
	}
	
	public PermissionFile intermediateFile() {
		return intermediateFile;
	}
}
