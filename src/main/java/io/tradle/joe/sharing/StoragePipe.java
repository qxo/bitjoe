package io.tradle.joe.sharing;

import io.netty.handler.codec.http.QueryStringEncoder;
import io.netty.util.CharsetUtil;
import io.tradle.joe.Config;
import io.tradle.joe.Joe;
import io.tradle.joe.TransactionData;
import io.tradle.joe.TransactionDataType;
import io.tradle.joe.events.KeyValue;
import io.tradle.joe.exceptions.StorageException;
import io.tradle.joe.utils.AESUtils;
import io.tradle.joe.utils.ECUtils;
import io.tradle.joe.utils.Gsons;
import io.tradle.joe.utils.HttpResponseData;
import io.tradle.joe.utils.TransactionUtils;
import io.tradle.joe.utils.Utils;

import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.wallet.DecryptingKeyBag;
import org.bitcoinj.wallet.KeyBag;
import org.h2.security.SHA256;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.util.encoders.Hex;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class StoragePipe {
	
	private static final Logger logger = LoggerFactory.getLogger(StoragePipe.class);
	public static final Charset FILE_ENCODING = CharsetUtil.UTF_8;

	private final Wallet wallet;
	private final NetworkParameters params;
	private final JsonParser jsonParser;

	public StoragePipe(Wallet wallet) {
		this.wallet = wallet;
		jsonParser = new JsonParser();
		params = wallet.getNetworkParameters();
	}

	public KeyValue receiveData(Transaction tx) {
		TransactionData data = TransactionUtils.getDataFromTransaction(tx);
		if (data == null)
			return null;
		
		return receiveData(tx, data);
	}
	
	public List<KeyValue> receiveData(List<Transaction> txs) {
		int numTxs = txs.size();
		List<TransactionData> tData = new ArrayList<TransactionData>();
		List<String> hashes = new ArrayList<String>();

		// build list of keys to batch query keeper for:
		
		StringBuilder intermediateKeys = new StringBuilder();
		for (Transaction t: txs) {
			TransactionData td = TransactionUtils.getDataFromTransaction(t);
			tData.add(td);
			
			String keyString = keyToString(td.data());
			hashes.add(keyString);
			intermediateKeys.append(keyString);
			intermediateKeys.append(",");
		}

		if (intermediateKeys.length() == 0)
			return null;
		
		JsonArray iFiles = fetchFiles(intermediateKeys.substring(0, intermediateKeys.length() - 1));
		List<String> decryptionKeys = new ArrayList<String>();

		// go through retrieved data and find decryption keys from intermediate files:
		
		for (int i = 0; i < numTxs; i++) {
			String iFile = iFiles.get(i).getAsString();
			String dKey = null;
			if (iFile != null) {
				Transaction t = txs.get(i);
				if (tData.get(i).type() == TransactionDataType.ENCRYPTED_SHARE) {
					PermissionFileData iData = getIntermediateFileData(t, iFile);
//					byte[] fileHash = keyToBytes(iData.fileHash());
					hashes.set(i, iData.fileHash());
					dKey = iData.decryptionKey();
				}
			}
			
			decryptionKeys.add(dKey);
		}
			
		// for files that were just intermediate files, fetch the actual file they point to:
		
		StringBuilder keys = new StringBuilder();
		for (int i = 0; i < numTxs; i++) {
			String dKey = decryptionKeys.get(i);
			if (dKey != null) {
				keys.append(hashes.get(i));
				keys.append(",");
			}
		}
		
		JsonArray files = null;
		if (keys.length() != 0)
			files = fetchFiles(keys.substring(0, keys.length() - 1));			

		// merge the cleartext-stored files with the encrypted files to preserve order by transaction time
		List<KeyValue> data = new ArrayList<KeyValue>();
		for (int i = 0, j = files.size(); i < numTxs; i++) {
			String dKey = decryptionKeys.get(i);
			String file = null;
			if (dKey == null) {
				file = tryGetAsString(iFiles, i);
				if (file != null)
					file = decryptFile(file, dKey);
			}
			else {
				file = tryGetAsString(files, j++);
			}
			
			data.add(new KeyValue(hashes.get(i), file));
		}

		return data;
	}

	private static String tryGetAsString(JsonArray arr, int idx) {
		if (arr == null)
			return null;
		
		JsonElement j = arr.get(idx);
		if (j == null)
			return null;
		
		return j.getAsString();
	}
	
	public KeyValue receiveData(Transaction tx, TransactionData data) {
		String intermediateFile = fetchFile(data.data());
		if (intermediateFile == null)
			return null;

		if (data.type() == TransactionDataType.CLEARTEXT_STORE)
			return new KeyValue(keyToString(data.data()), intermediateFile);
		
		PermissionFileData iData = getIntermediateFileData(tx, intermediateFile);
		String dKey = iData.decryptionKey();
		byte[] fileHash = keyToBytes(iData.fileHash());
		String file = fetchFile(fileHash);
		if (file == null)
			return null;
		
		file = decryptFile(file, dKey);
		return new KeyValue(iData.fileHash(), file);
	}
	
	private PermissionFileData getIntermediateFileData(Transaction tx, String encryptedIntermediateFile) {
		byte[] secret = getSharedSecret(tx);
		KeyParameter key = new KeyParameter(secret);
		String decryptedIntermediateFile = decryptFile(encryptedIntermediateFile, key);
		System.out.println("Decrypted intermediate file: " + decryptedIntermediateFile);
		return Gsons.ugly().fromJson(decryptedIntermediateFile, PermissionFileData.class);
	}

	private JsonElement parseJson(String file) {
		return jsonParser.parse(file);
	}

	private String fetchFile(byte[] hash) {
		String hashString = TransactionUtils.transactionDataToString(hash);

		Config config = Joe.JOE.config();
		QueryStringEncoder qs = new QueryStringEncoder(config.keepers().get(0).toString());
		qs.addParam("key", hashString);

		HttpResponseData response = null;
		try {
			response = Utils.get(qs.toUri());
		} catch (URISyntaxException e) {
			logger.error("Constructed bad URI: " + qs, e);
			throw new StorageException("constructed bad URI for fetching file from keeper", e);
		}

		if (response.code() > 399) {
			logger.error("Hash not found in storage: " + hashString);
			return null;
		}
		
		return response.response(); 
	}
	
	private JsonArray fetchFiles(String keysCsv) {
		Config config = Joe.JOE.config();
		QueryStringEncoder qs = new QueryStringEncoder(config.keepers().get(0).toString());
		qs.addParam("keys", keysCsv);

		HttpResponseData response = null;
		try {
			response = Utils.get(qs.toUri());
		} catch (URISyntaxException e) {
			logger.error("Constructed bad URI: " + qs, e);
			throw new StorageException("constructed bad URI for fetching file from keeper", e);
		}

		if (response.code() > 399) {
			logger.error("Hashes not found in storage: " + keysCsv);
			return null;
		}
		
		return (JsonArray) new JsonParser().parse(response.response()); 		
	}
	
	private String decryptFile(String file, String decryptionKey) {
		return decryptFile(file, new KeyParameter(encryptionKeyToBytes(decryptionKey)));
	}

	private String decryptFile(String encryptedFile, KeyParameter decryptionKey) {
		if (decryptionKey == null)
			return encryptedFile;
		
		byte[] encrypted = ciphertextToBytes(encryptedFile);
		byte[] decrypted = null;
		try {
			decrypted = AESUtils.decrypt(encrypted, decryptionKey);
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to decrypt data with provided decryption key: " + encryptedFile, e);
		}
		
		return fileDataToString(decrypted);
	}
	
	public static byte[] encryptFile(String file, KeyParameter key) {
		return AESUtils.encrypt(fileStringToBytes(file), key);
	}
	
	private byte[] getSharedSecret(Transaction tx) {
		TransactionInput in = tx.getInput(0);
		List<TransactionOutput> toMe = TransactionUtils.getReceived(wallet, tx);
		TransactionOutput out = null;
		for (TransactionOutput o: toMe) {
			if (o.getValue().equals(ShareRequest.SHARING_COST)) {
				out = o;
				break;
			}
		}

		if (out == null)
			out = toMe.get(0);
		
		byte[] theirPubKey = in.getScriptSig().getPubKey();
		KeyBag keyBag = new DecryptingKeyBag(wallet, null);
		ECKey myKey = keyBag.findKeyFromPubHash(out.getAddressFromP2PKHScript(params).getHash160());
		BigInteger myPrivKey = myKey.getPrivKey();
		return ECUtils.getSharedSecret(ECKey.fromPublicOnly(theirPubKey).getPubKeyPoint(), myPrivKey);
	}

	public byte[] getData(String key) {
        QueryStringEncoder qs = new QueryStringEncoder(Joe.JOE.config().keepers().get(0).toString());
        qs.addParam("key", key);
        
        HttpResponseData response = null;
        try {
			response = Utils.get(qs.toUri());
        } catch (URISyntaxException e) {
			logger.error("Constructed bad URI: " + qs, e);
			return null;
		} 
        
		if (response.code() > 399) {
			logger.error("Hash not found in storage: " + key);
			return null;
		}
			
		System.out.println("Hash found in storage!");
		System.out.println("Key: " + key);
		System.out.println("Value: " + response.response());
		
		return response.response().getBytes(CharsetUtil.UTF_8);
	}

//	public static HttpResponseData store(byte[] hash, byte[] data) {
//		return store(keyToString(hash), ciphertextBytesToString(data));
//	}
	
	public static HttpResponseData store(String key, String value) {
		Config.AddressConfig keeper = Joe.JOE.config().keepers().get(0);
		QueryStringEncoder url = new QueryStringEncoder(keeper.toString());
		url.addParam("key", key);
		url.addParam("val", value);
		
		try {
			return io.tradle.joe.utils.Utils.get(url.toUri());
		} catch (URISyntaxException e) {
			// should never happen...
			throw new IllegalArgumentException("invalid keeper url", e);
		}
	}

	public static String getStorageKeyStringFor(String file) {
		return keyToString(
				getStorageKeyFor(
				  fileStringToBytes(file)));
	}

	public static String getStorageKeyStringFor(byte[] data) {
		return keyToString(
				getStorageKeyFor(data));
	}

	public static byte[] getStorageKeyFor(byte[] data) {
		return SHA256.getHash(data, false);
	}

	public static byte[] keyToBytes(String key) {
		try {
			return Base58.decode(key);
		} catch (AddressFormatException e) {
			throw new IllegalArgumentException("Provided key was not in Base58 encoding", e);
		}
	}
	
	public static String keyToString(byte[] key) {
		return Base58.encode(key);
	}

	public static String encryptionKeyToString(byte[] key) {
		return new String(Hex.encode(key));
	}

	public static byte[] encryptionKeyToBytes(String key) {
		return Hex.decode(key);
	}

	public static String ciphertextBytesToString(byte[] ciphertextBytes) {
		return Base64.encodeBase64String(ciphertextBytes);
	}

	public static byte[] ciphertextToBytes(String ciphertext) {
		return Base64.decodeBase64(ciphertext);
	}

	public static String fileDataToString(byte[] fileData) {
		return new String(fileData, FILE_ENCODING);
	}
	
	public static byte[] fileStringToBytes(String file) {
		return file.getBytes(FILE_ENCODING);
	}

}
