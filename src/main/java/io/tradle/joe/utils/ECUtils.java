package io.tradle.joe.utils;

import java.math.BigInteger;
import java.util.Arrays;

import org.apache.commons.codec.binary.Hex;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Utils;
import org.bitcoinj.params.TestNet3Params;
import org.h2.security.SHA256;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.math.ec.ECPoint;

public class ECUtils {

	private static final BigInteger TWO = BigInteger.valueOf(2);
	
	/**
	 * @param pubKey - public key
	 * @param privKey - private key
	 * @return unique shared secret key based on the combination of public key pubKey and private key privKey
	 */
	public static byte[] getSharedSecret(ECPoint pubKey, BigInteger privKey) {
		ECPoint sharedSecret = pubKey.multiply(privKey).normalize();
		byte[] shared = SHA256.getHash(sharedSecret.getEncoded(true), false);
		return shared;
	}	

	/**
	 * Create a "clue" for a given recipient to allow them to reconstruct a shared secret
	 * @param recipientPubKey
	 * @param secretKey
	 * @return
	 */
	public static ECPoint createClue(ECPoint recipientPubKey, ECKey secretKey) {
		return createClue(recipientPubKey, secretKey.getPrivKey());
	}

	public static ECPoint createClue(ECPoint recipientPubKey, BigInteger secretKey) {
		return recipientPubKey.multiply(secretKey);
	}

	public static ECPoint recoverSharedSecret(BigInteger recipientPrivKey, ECPoint clue) {
		return clue.multiply(recipientPrivKey.modInverse(ECKey.CURVE.getN()));
	}
	
	/**
	 * See ECUtils.getSharedSecret
	 * 
	 * @param pubKey - public key
	 * @param privKey - private key
	 * @return KeyParameter for unique shared secret key based on the combination of public key pubKey and private key privKey
	 */
	public static KeyParameter getSharedSecretKeyParameter(ECPoint pubKey, BigInteger privKey) {
		return new KeyParameter(getSharedSecret(pubKey, privKey));
	}

	public static ECPoint pubKeyToPoint(String pubKey) {
		ECKey ecKey = ECKey.fromPublicOnly(pubKey.getBytes());
		return ecKey.getPubKeyPoint();
	}

	public static String toPubKeyString(ECPoint point) {
		return Utils.HEX.encode(point.getEncoded());
	}

	public static String toPubKeyString(ECKey key) {
		return toPubKeyString(key.getPubKeyPoint());
	}
}
