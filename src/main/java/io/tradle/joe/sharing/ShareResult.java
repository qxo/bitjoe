package io.tradle.joe.sharing;

import java.util.Map;

import org.bitcoinj.core.Wallet.SendResult;

public class ShareResult {

	private final Map<String, Permission> results;
	private final String fileKey;
	private SendResult fileStoreSendResult;

	public ShareResult(String fileKey, Map<String, Permission> results) {
		this.fileKey = fileKey;
		this.results = results;
	}

	public ShareResult(String fileKey, Map<String, Permission> results, SendResult sendResult) {
		this(fileKey, results);
		this.fileStoreSendResult = sendResult;
	}

	public Map<String, Permission> results() {
		return results;
	}
	
	public String fileKey() {
		return fileKey;
	}
	
	/**
	 * @return SendResult from storing file hash directly in the blockchain - only for CLEARTEXT_STORE type data
	 */
	public SendResult sendResult() {
		return fileStoreSendResult;
	}
}
