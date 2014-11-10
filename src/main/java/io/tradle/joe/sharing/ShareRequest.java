package io.tradle.joe.sharing;

import io.tradle.joe.exceptions.StorageException;
import io.tradle.joe.utils.AESUtils;
import io.tradle.joe.utils.ECUtils;
import io.tradle.joe.utils.HttpResponseData;
import io.tradle.joe.utils.TransactionUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.Wallet.SendResult;
import org.bitcoinj.wallet.KeyChain.KeyPurpose;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.math.ec.ECPoint;

/**
 * Stores data and optionally shares it with a limited number of recipients. Handles encryption with shared secrets derived from receipients' public keys.
 */
public class ShareRequest {

	private Wallet wallet;
	private boolean storeInCleartext;
	private List<String> shareWith; // public keys of recipients
	private KeyParameter encryptionKey;
	private String encryptionKeyStr;
	private byte[] data;
	private byte[] encryptedData;
	private byte[] fileHash;
	private NetworkParameters params;

	// results
	private Map<String, Permission> results;
	private String fileHashStr;
	private HttpResponseData storeResponse;
	
	public static final Coin SHARING_COST = Transaction.MIN_NONDUST_OUTPUT.multiply(3);
	
	public ShareRequest(Wallet wallet) {
		this.wallet = wallet;
		this.params = wallet.getNetworkParameters();
		this.shareWith = new ArrayList<String>();
	}
	
	public ShareRequest store(byte[] data) {
		this.data = data;
		return this;
	}
	
	/**
	 * Specify a list of the public keys of the recipients  
	 * @param pubKeys
	 * @return
	 */
	public ShareRequest shareWith(List<String> pubKeys) {
		this.shareWith.addAll(pubKeys);
		return this;
	}
	
	/**
	 * Specify the public key of a recipient  
	 * @param pubKey
	 * @return
	 */
	public ShareRequest shareWith(String pubKey) {
		this.shareWith.add(pubKey);
		return this;
	}

	
	public ShareRequest encrypt(KeyParameter key) {
		this.encryptionKey = key;
		return this;
	}
		
	public ShareRequest cleartext(boolean storeInCleartext) {
		this.storeInCleartext = storeInCleartext;
		return this;
	}
		
	public ShareResult execute() throws InsufficientMoneyException {
		this.results = new HashMap<String, Permission>();

		storeResponse = store();
		int code = storeResponse.code();
		if (code < 0)
			throw new StorageException("failed to reach keeper, transaction aborted");
		else if (code > 399) {
			if (code == 409) {
				// TODO: find transaction in block chain with this OP_RETURN, return associated data
			}

			throw new StorageException("transaction refused by keeper network: " + code + " " + storeResponse.response());
		}		
		
		if (shareWith.isEmpty()) {
			if (!storeInCleartext) {
				ECKey newKey = wallet.freshKey(KeyPurpose.AUTHENTICATION);
				shareWith(ECUtils.toPubKeyString(newKey));
			}
		}
			
		for (String pubKey: shareWith) {
			doShareWith(pubKey);
		}
		
		return new ShareResult(fileHashStr, results);
	}

	private HttpResponseData store() {
		if (storeInCleartext) {
			encryptedData = data;
		}
		else {
			if (this.encryptionKey == null)
				encryptionKey = new KeyParameter(AESUtils.generateKey().getEncoded());
			
			encryptionKeyStr = StoragePipe.encryptionKeyToString(encryptionKey.getKey());
			encryptedData = AESUtils.encrypt(data, encryptionKey);
		}
		
		fileHash = StoragePipe.getStorageKeyFor(data);
		fileHashStr = StoragePipe.keyToString(fileHash);
		
		String fileStr = storeInCleartext ? 
							StoragePipe.fileDataToString(data) : 
							StoragePipe.ciphertextBytesToString(encryptedData);
							
		return StoragePipe.store(fileHashStr, fileStr);
	}

	private void doShareWith(String pubKey) throws InsufficientMoneyException {
		ECKey ecKey = ECKey.fromPublicOnly(Utils.HEX.decode(pubKey));
		ECPoint pubKeyPoint = ecKey.getPubKeyPoint();
		Address to = ecKey.toAddress(params);
		
		Wallet.SendRequest req = Wallet.SendRequest.to(to, SHARING_COST);
		req.shuffleOutputs = false;
		wallet.completeTx(req);

		TransactionInput in = req.tx.getInput(0);
		ECKey key = TransactionUtils.getKey(wallet, in);
		wallet.getActiveKeychain().markPubKeyAsUsed(key.getPubKey());
		BigInteger privKey = key.getPrivKey();
		
        byte[] secret = ECUtils.getSharedSecret(pubKeyPoint, privKey);
		KeyParameter iKey = new KeyParameter(secret);
		PermissionFile permission = new PermissionFile(fileHashStr, encryptionKeyStr);
		permission.encrypt(iKey);
		
		storeResponse = StoragePipe.store(permission.hash(), permission.ciphertext());
		int code = storeResponse.code();
		if (code < 0)
			throw new StorageException("failed to reach keeper, transaction aborted");
		else if (code > 399) {
			if (code == 409) {
				// TODO: find transaction in block chain with this OP_RETURN, return associated data
			}
			else
				throw new StorageException("transaction refused by keeper network: " + code + " " + storeResponse.response());
		}		

		// this is pretty ugly, but currently there's no convenient way to build a transaction without completing it
		// copy stuff over from the 'completed' transaction, and add data. The transaction we used as a template will be thrown away
		Transaction copy = new Transaction(params);
		
		for (TransactionOutput o: req.tx.getOutputs()) {
			copy.addOutput(o);
		}
		
		for (TransactionInput i: req.tx.getInputs()) {
			copy.addInput(i);
		}
		
		TransactionUtils.addDataToTransaction(copy, permission.hashBytes());
		SendResult result = wallet.sendCoins(Wallet.SendRequest.forTx(copy));
		
		Permission shareResult = new Permission(result, permission);
		this.results.put(pubKey, shareResult);
	}	
}
