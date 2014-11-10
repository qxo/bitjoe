package io.tradle.joe.sharing;

import io.netty.util.CharsetUtil;
import io.tradle.joe.utils.AESUtils;
import io.tradle.joe.utils.Gsons;

import java.nio.charset.Charset;

import org.apache.commons.codec.binary.Base64;
import org.h2.security.SHA256;
import org.spongycastle.crypto.params.KeyParameter;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

public class PermissionFile {

	private PermissionFileData fileData;
	private JsonElement json;
	private String ciphertext;
	private byte[] ciphertextBytes;
	private byte[] hashBytes;
	private String hash;

	public PermissionFile(String fileHash, String decryptionKey) {
		this.fileData = new PermissionFileData(fileHash, decryptionKey);
		this.json = Gsons.ugly().toJsonTree(fileData, PermissionFileData.class);
	}

	public JsonElement toJson() {
		return json;
	}
	
	public void encrypt(KeyParameter key) {
		this.ciphertext = null;
		this.hash = null;
		this.ciphertextBytes = StoragePipe.encryptFile(this.json.toString(), key);
		this.hashBytes = StoragePipe.getStorageKeyFor(ciphertextBytes);
	}

	public String ciphertext() {
		if (this.ciphertextBytes == null)
			throw new IllegalStateException("This file is not encrypted, call encrypt() first");
		
		if (ciphertext == null)
			ciphertext = StoragePipe.ciphertextBytesToString(ciphertextBytes);
		
		return ciphertext;
	}

	public byte[] ciphertextBytes() {
		return ciphertextBytes;
	}

	public byte[] hashBytes() {
		return hashBytes;
	}

	public String hash() {
		if (hash == null)
			hash = StoragePipe.keyToString(hashBytes);
		
		return hash;
	}
}
