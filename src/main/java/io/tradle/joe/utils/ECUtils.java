package io.tradle.joe.utils;

import java.math.BigInteger;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.bitcoinj.core.ECKey;
import org.h2.security.SHA256;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.math.ec.ECPoint;


public class ECUtils {

	/**
	 * @param pubKey - public key
	 * @param privKey - private key
	 * @return unique shared secret key based on the combination of public key pubKey and private key privKey
	 */
	public static byte[] getSharedSecret(ECPoint pubKey, BigInteger privKey) {
		ECPoint sharedSecret = pubKey.multiply(privKey);
		byte[] point = sharedSecret.getEncoded(true);
		byte[] b64point = Base64.encodeBase64(point);
		char[] b64chars = new char[b64point.length];
		for (int i = 0; i < b64point.length; i++) {
			b64chars[i] = (char)b64point[i];
		}

		return SHA256.getHash(AESUtils.generateKey(b64chars).getEncoded(), true);
	}
	
	/**
	 * See ECUtils.getSharedSecret
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
	
	public static String ecPointToHex(ECPoint point) {
		return Hex.encodeHexString(point.getEncoded(false));
	}
}
