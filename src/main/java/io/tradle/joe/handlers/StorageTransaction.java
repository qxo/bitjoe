package io.tradle.joe.handlers;

import static com.google.common.base.Preconditions.checkArgument;
import io.netty.util.CharsetUtil;
import io.tradle.joe.Joe;
import io.tradle.joe.TransactionRequest;
import io.tradle.joe.utils.AESUtils;
import io.tradle.joe.utils.Utils;

import java.util.Arrays;

import org.bitcoinj.core.ECKey;
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

		KeyParameter sharedSecret = null;
		if (!Utils.isTruthy(req.param("cleartext"))) {
			ECKey encryptionKeyBase = new ECKey();
			Joe.JOE.wallet().importKey(encryptionKeyBase); // TODO: store separately from signing keys
			
			// TODO: check if this is complete bullshit:
			byte[] secret = SHA256.getHash(encryptionKeyBase.getPubKeyHash(), false);
			sharedSecret = new KeyParameter(secret);
		}
		
		if (sharedSecret == null)
			encryptedBytes = unencryptedBytes;
		else {
			encryptedBytes = AESUtils.encrypt(unencryptedBytes, sharedSecret, AESUtils.AES_INITIALISATION_VECTOR);
	        
	        // Check that the encryption is reversible
	        byte[] rebornBytes = AESUtils.decrypt(encryptedBytes, sharedSecret, AESUtils.AES_INITIALISATION_VECTOR);
	         
	        checkArgument(Arrays.equals(unencryptedBytes, rebornBytes), "The encryption was not reversible so aborting.");
		}
		
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
	
	public String getEncryptedString() {
		if (encryptedString == null)
			encryptedString = new String(getEncrypted(), CharsetUtil.UTF_8);
		
		return encryptedString;
	}
	
	public String getUnencryptedString() {
		if (unencryptedString == null)
			unencryptedString = new String(getUnencrypted(), CharsetUtil.UTF_8);
		
		return unencryptedString;
	}

	/**
	 * @return SHA256 hash in base58 of the transaction data
	 */
	public String getHashString() {
		if (hashString == null)
			hashString = Utils.toBase58String(getHash());
		
		return hashString;
	}

	public TransactionRequest transactionRequest() {
		return req;
	}
	
}
