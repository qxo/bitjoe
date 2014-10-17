package io.tradle.joe;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.WalletExtension;

public class FileKeysExtension implements WalletExtension {

    static final String EXTENSION_ID = FileKeysExtension.class.getName();
    static final SecureRandom random = new SecureRandom();
    
    private SecretKey key;

    public FileKeysExtension() {
    	try {
			KeyGenerator gen = KeyGenerator.getInstance("AES");
			gen.init(random);
			key = gen.generateKey();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("AES not supported, failed to generate key");
		}
    }
    
    public String getWalletExtensionID() {
		return EXTENSION_ID;
	}

	public boolean isWalletExtensionMandatory() {
		return true;
	}

	public byte[] serializeWalletExtension() {
		return key.getEncoded();
	}

	public void deserializeWalletExtension(Wallet wallet, byte[] data) throws Exception {
		key = new SecretKeySpec(data, 0, data.length, "AES");
	}

	public SecretKey key() {
		return key;
	}
}
