package io.tradle.joe.handlers;

import static com.google.common.base.Preconditions.checkArgument;
import io.netty.util.CharsetUtil;
import io.tradle.joe.Joe;
import io.tradle.joe.TransactionRequest;
import io.tradle.joe.utils.AESUtils;
import io.tradle.joe.utils.ECUtils;

import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;
import org.bitcoinj.core.Base58;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.wallet.KeyChain.KeyPurpose;
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
		// HACK:
		KeyParameter sharedSecret = Joe.JOE.getSharedSecretKeyParameter(req.getDestinationKey(), Joe.JOE.wallet().currentKey(KeyPurpose.CHANGE));
		
        encryptedBytes = AESUtils.encrypt(unencryptedBytes, sharedSecret, AESUtils.AES_INITIALISATION_VECTOR);
        
        // Check that the encryption is reversible
        byte[] rebornBytes = AESUtils.decrypt(encryptedBytes, sharedSecret, AESUtils.AES_INITIALISATION_VECTOR);
         
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

	public TransactionRequest transactionRequest() {
		return req;
	}
	
}
