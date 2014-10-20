package io.tradle.joe.handlers;

import static com.google.common.base.Preconditions.checkArgument;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.CharsetUtil;
import io.tradle.joe.FileKeysExtension;
import io.tradle.joe.Joe;
import io.tradle.joe.TransactionRequest;
import io.tradle.joe.utils.AESUtils;

import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;
import org.bitcoinj.core.Base58;
import org.h2.security.SHA256;
import org.spongycastle.crypto.params.KeyParameter;

public class StorageTransaction {

	private final TransactionRequest req;
	private final byte[] hash;
	private final byte[] unencryptedBytes;
	private final byte[] encryptedBytes;
	
	private String hashString;
	private String unencryptedString;
	private String encryptedString;

	public StorageTransaction(TransactionRequest req, byte[] unencryptedBytes) {
		this.req = req;
		this.unencryptedBytes = unencryptedBytes;
		
	   	// encrypt & store tx
        // Create an AES encoded version of the unencryptedBytes, using the credentials
    	FileKeysExtension fileKeys = (FileKeysExtension) Joe.JOE.wallet().getExtensions().get(FileKeysExtension.EXTENSION_ID);
    	KeyParameter keyParameter = new KeyParameter(fileKeys.key().getEncoded());
    	
        encryptedBytes = AESUtils.encrypt(unencryptedBytes, keyParameter, AESUtils.AES_INITIALISATION_VECTOR);
        
        // Check that the encryption is reversible
        byte[] rebornBytes = AESUtils.decrypt(encryptedBytes, keyParameter, AESUtils.AES_INITIALISATION_VECTOR);
         
        checkArgument(Arrays.equals(unencryptedBytes, rebornBytes), "The encryption was not reversible so aborting.");
    	
    	// hash tx
    	hash = SHA256.getHash(encryptedBytes, false);
	}

	public byte[] getUnencrypted() {
		return Arrays.copyOf(unencryptedBytes, unencryptedBytes.length);
	}

	public byte[] getEncrypted() {
		return Arrays.copyOf(encryptedBytes, encryptedBytes.length);
	}
	
	public byte[] getHash() {
		return Arrays.copyOf(hash, hash.length);
	}
	
	private String toBase64String(byte[] bytes) {
		return new String(Base64.encodeBase64(bytes));
	}

	private String toBase58String(byte[] bytes) {
		return Base58.encode(bytes);
	}

	private String toUTF8String(byte[] bytes) {
		return new String(bytes, CharsetUtil.UTF_8);
	}
	
	public String getEncryptedString() {
		if (encryptedString == null)
			encryptedString = toBase58String(getEncrypted());
		
		return encryptedString;
	}
	
	public String getUnencryptedString() {
		if (unencryptedString == null)
			unencryptedString = toUTF8String(getUnencrypted());
		
		return unencryptedString;
	}

	public String getHashString() {
		if (hashString == null)
			hashString = toBase58String(getHash());
		
		return hashString;
	}

	public TransactionRequest httpRequest() {
		return req;
	}
	
}
