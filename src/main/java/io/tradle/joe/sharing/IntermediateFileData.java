package io.tradle.joe.sharing;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

public class IntermediateFileData {

	private String fileHash;
	private String decryptionKey;

	public IntermediateFileData(String fileHash, String decryptionKey) {
		this.fileHash = fileHash;
		this.decryptionKey = decryptionKey;		
	}

	public String fileHash() {
		return fileHash;
	}
	
	public String decryptionKey() {
		return decryptionKey;
	}
}
