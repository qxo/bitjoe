package io.tradle.joe.utils;

import java.math.BigInteger;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.bitcoinj.core.ECKey;
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
		ECPoint sharedSecret = pubKey.multiply(privKey);
		byte[] point = sharedSecret.getEncoded(true);
		byte[] b64point = Base64.encodeBase64(point);
		char[] b64chars = new char[b64point.length];
		for (int i = 0; i < b64point.length; i++) {
			b64chars[i] = (char) b64point[i];
		}

		return SHA256.getHash(AESUtils.generateKey(b64chars).getEncoded(), true);
	}
	
	/**
	 * Create a "clue" for a given recipient to allow them to reconstruct 
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

	public static String ecPointToHex(ECPoint point) {
		return Hex.encodeHexString(point.getEncoded(false));
	}

//	public static void main(String[] args) {
//		try {
//			//
//			// a side
//			//
//			
////			ECKey a = new ECKey();
////			ECKey b = new ECKey();
//			
//			SecureRandom secureRandom = new SecureRandom();
//	        ECKeyPairGenerator generator = new ECKeyPairGenerator();
//	        ECKeyGenerationParameters keygenParams = new ECKeyGenerationParameters(ECKey.CURVE, secureRandom);
//	        generator.init(keygenParams);
//	        AsymmetricCipherKeyPair keypair = generator.generateKeyPair();
//	        ECPrivateKeyParameters privParams = (ECPrivateKeyParameters) keypair.getPrivate();
//	        ECPublicKeyParameters pubParams = (ECPublicKeyParameters) keypair.getPublic();
//	        
//			//
//			// stream test
//			//
//			Cipher c1 = Cipher.getInstance("ECIES", "BC");
//			Cipher c2 = Cipher.getInstance("ECIES", "BC");
//
////			IESCipher.ECIESwithAES();
//			
//			IEKeySpec c1Key = new IEKeySpec(new JCEECPrivateKey(new ECPrivateKey(privParams.getD())), new JCEECPublicKey("EC", pubParams));
//			IEKeySpec c2Key = new IEKeySpec(new ECPrivateKey(b.getPrivKey()), );
//
//			byte[] d = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
//			byte[] e = new byte[] { 8, 7, 6, 5, 4, 3, 2, 1 };
//
//			IESParameterSpec param = new IESParameterSpec(d, e, 128);
//
//			c1.init(Cipher.ENCRYPT_MODE, c1Key, param);
//
//			c2.init(Cipher.DECRYPT_MODE, c2Key, param);
//
//			byte[] message = Hex.decode("1234567890abcdef");
//
//			byte[] out1 = c1.doFinal(message, 0, message.length);
//
//			byte[] out2 = c2.doFinal(out1, 0, out1.length);
//
//			assert(Arrays.equals(out1, out2));
//		} catch (Exception ex) {
//			ex.printStackTrace();
//		}
//	}	
}
