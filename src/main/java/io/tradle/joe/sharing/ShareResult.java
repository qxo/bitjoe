package io.tradle.joe.sharing;

import io.tradle.joe.utils.HttpResponseData;

import java.util.Map;

public class ShareResult {

	private final Map<String, Permission> results;
	private final String fileKey;

	public ShareResult(String fileKey, Map<String, Permission> results) {
		this.fileKey = fileKey;
		this.results = results;
	}

	public Map<String, Permission> results() {
		return results;
	}
	
	public String fileKey() {
		return fileKey;
	}
}
