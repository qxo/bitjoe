package io.tradle.joe.sharing;

public class PermissionFileData {

	private String fileHash;
	private String decryptionKey;

	public PermissionFileData(String fileHash, String decryptionKey) {
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
