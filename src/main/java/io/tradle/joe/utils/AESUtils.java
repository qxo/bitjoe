package io.tradle.joe.utils;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.bitcoinj.crypto.KeyCrypterException;
import org.spongycastle.crypto.BufferedBlockCipher;
import org.spongycastle.crypto.engines.AESFastEngine;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;

/**
 * From MultiBit-hd: https://github.com/bitcoin-solutions/multibit-hd/blob/develop/mbhd-brit/src/main/java/org/multibit/hd/brit/crypto/AESUtils.java
 * <p>Utility class to provide the following to BRIT API:</p>
 * <ul>
 * <li>Encryption and decryption using AES</li>
 * </ul>
 *
 * @since 0.0.1
 */
public class AESUtils {

  private static final SecureRandom random = new SecureRandom();
	
  /**
   * Key length in bytes.
   */
  public static final int KEY_LENGTH = 32; // = 256 bits.

  /**
   * The size of an AES block in bytes.
   * This is also the length of the initialisation vector.
   */
  public static final int BLOCK_LENGTH = 16;  // = 128 bits.

  /**
   * The initialisation vector to use for AES encryption of output files (such as wallets)
   * There is no particular significance to the value of these bytes
   */
  public static final byte[] AES_INITIALISATION_VECTOR = new byte[]{(byte) 0xa3, (byte) 0x44, (byte) 0x39, (byte) 0x1f, (byte) 0x53, (byte) 0x83, (byte) 0x11,
    (byte) 0xb3, (byte) 0x29, (byte) 0x54, (byte) 0x86, (byte) 0x16, (byte) 0xc4, (byte) 0x89, (byte) 0x72, (byte) 0x3e};

  /**
   * The salt used for deriving the KeyParameter from the credentials in AES encryption for wallets
   */
  public static final byte[] SCRYPT_SALT = new byte[]{(byte) 0x35, (byte) 0x51, (byte) 0x03, (byte) 0x80, (byte) 0x75, (byte) 0xa3, (byte) 0xb0, (byte) 0xc5};
  
  /**
   * Utilities have private constructors
   */
  private AESUtils() {
  }

  /**
   * Password based encryption using AES - CBC 256 bits.
   *
   * @param plainBytes           The unencrypted bytes for encryption
   * @param aesKey               The AES key to use for encryption
   * @param initialisationVector The initialisationVector to use whilst encrypting
   *
   * @return The encrypted bytes
   */
  public static byte[] encrypt(byte[] plainBytes, KeyParameter aesKey, byte[] initialisationVector) throws KeyCrypterException {

    checkNotNull(plainBytes);
    checkNotNull(aesKey);
    checkNotNull(initialisationVector);
    checkState(initialisationVector.length == BLOCK_LENGTH, "The initialisationVector must be " + BLOCK_LENGTH + " bytes long.");

	System.out.println("Encrypting: " + Arrays.toString(plainBytes));
	System.out.println("Encrypting with: " + Arrays.toString(aesKey.getKey()));

    try {
      ParametersWithIV keyWithIv = new ParametersWithIV(aesKey, initialisationVector);

      // Encrypt using AES
      BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESFastEngine()));
      cipher.init(true, keyWithIv);
      byte[] encryptedBytes = new byte[cipher.getOutputSize(plainBytes.length)];
      final int processLength = cipher.processBytes(plainBytes, 0, plainBytes.length, encryptedBytes, 0);
      final int doFinalLength = cipher.doFinal(encryptedBytes, processLength);

      return Arrays.copyOf(encryptedBytes, processLength + doFinalLength);
    } catch (Exception e) {
      throw new KeyCrypterException("Could not encrypt bytes.", e);
    }

  }
  
  public static byte[] encrypt(byte[] data, byte[] aesKey) throws KeyCrypterException {
	  return encrypt(data, new KeyParameter(aesKey));
  }

  public static byte[] encrypt(byte[] data, KeyParameter aesKey) throws KeyCrypterException {
	  return encrypt(data, aesKey, AES_INITIALISATION_VECTOR);
  }

  /**
   * Decrypt bytes previously encrypted with this class.
   *
   * @param encryptedBytes       The encrypted bytes required to decrypt
   * @param aesKey               The AES key to use for decryption
   * @param initialisationVector The initialisationVector to use whilst decrypting
   *
   * @return The decrypted bytes
   *
   * @throws KeyCrypterException if bytes could not be decoded to a valid key
   */

  public static byte[] decrypt(byte[] encryptedBytes, KeyParameter aesKey, byte[] initialisationVector) throws KeyCrypterException {
    checkNotNull(encryptedBytes);
    checkNotNull(aesKey);
    checkNotNull(initialisationVector);

	System.out.println("Decrypting: " + Arrays.toString(encryptedBytes));
	System.out.println("Decrypting with: " + Arrays.toString(aesKey.getKey()));

    try {
      ParametersWithIV keyWithIv = new ParametersWithIV(new KeyParameter(aesKey.getKey()), initialisationVector);

      // Decrypt the message.
      BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESFastEngine()));
      cipher.init(false, keyWithIv);

      int minimumSize = cipher.getOutputSize(encryptedBytes.length);
      byte[] outputBuffer = new byte[minimumSize];
      int length1 = cipher.processBytes(encryptedBytes, 0, encryptedBytes.length, outputBuffer, 0);
      int length2 = cipher.doFinal(outputBuffer, length1);
      int actualLength = length1 + length2;

      byte[] decryptedBytes = new byte[actualLength];
      System.arraycopy(outputBuffer, 0, decryptedBytes, 0, actualLength);

      return decryptedBytes;
    } catch (Exception e) {
      throw new KeyCrypterException("Could not decrypt bytes", e);
    }
  }
	
  public static byte[] decrypt(byte[] data, byte[] aesKey) throws KeyCrypterException {
	  return decrypt(data, new KeyParameter(aesKey));
  }

  public static byte[] decrypt(byte[] data, KeyParameter aesKey) throws KeyCrypterException {
	  return decrypt(data, aesKey, AES_INITIALISATION_VECTOR);
  }

  public static SecretKey generateKey(char[] password) {
	  try {
		  SecretKeyFactory kf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		  PBEKeySpec spec = new PBEKeySpec(password, SCRYPT_SALT, 8192, 256);
		  SecretKey tmp = kf.generateSecret(spec);
		  return new SecretKeySpec(tmp.getEncoded(), "AES");
	  } catch (Exception e) {
		  // should never happen but...
		  throw new IllegalArgumentException("failed to generate AES key from password", e);
	  }
  }
  
  public static SecretKey generateKey() {
		KeyGenerator gen;
		try {
			gen = KeyGenerator.getInstance("AES");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Failed to get generator for algorithm", e);
		}
		
		gen.init(random);
		return gen.generateKey();
  }

}
