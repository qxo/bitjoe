package io.tradle.joe.sharing;

import io.tradle.joe.exceptions.StorageException;
import io.tradle.joe.sharing.ShareRequest.Builder;
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
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.wallet.KeyChain.KeyPurpose;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.math.ec.ECPoint;

public class ShareRequest {

	private final Map<String, ShareResult> results;
	private final String fileHashStr;
	private final HttpResponseData storeResponse;
	public static final Coin SHARING_COST = Transaction.MIN_NONDUST_OUTPUT.multiply(3);
	
	public ShareRequest(Builder b) {
		this.results = b.results;
		this.storeResponse = b.storeResponse;
		this.fileHashStr = b.fileHashStr;
	}
	
	public Map<String, ShareResult> results() {
		return results;
	}
	
	public String fileKey() {
		return fileHashStr;
	}
	
	public HttpResponseData storeResponse() {
		return storeResponse;
	}
	
	public static class Builder {
		private Wallet wallet;
		private boolean storeInCleartext;
		private List<String> shareWith; // public keys of recipients
		private KeyParameter encryptionKey;
		private String encryptionKeyStr;
		private byte[] data;
		private byte[] encryptedData;
		private byte[] fileHash;
		private String fileHashStr;
		private Map<String, ShareResult> results;
		private HttpResponseData storeResponse;
		private NetworkParameters params;

		public Builder(Wallet wallet) {
			this.wallet = wallet;
			this.params = wallet.getNetworkParameters();
			this.shareWith = new ArrayList<String>();
		}
		
		public Builder store(byte[] data) {
			this.data = data;
			return this;
		}
		
		/**
		 * Specify a list of the public keys of the recipients  
		 * @param pubKeys
		 * @return
		 */
		public Builder shareWith(List<String> pubKeys) {
			this.shareWith.addAll(pubKeys);
			return this;
		}
		
		/**
		 * Specify the public key of a recipient  
		 * @param pubKey
		 * @return
		 */
		public Builder shareWith(String pubKey) {
			this.shareWith.add(pubKey);
			return this;
		}

		
		public Builder encrypt(KeyParameter key) {
			this.encryptionKey = key;
			return this;
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
		
		public Builder cleartext(boolean storeInCleartext) {
			this.storeInCleartext = storeInCleartext;
			return this;
		}

		private void doShareWith(String pubKey) throws InsufficientMoneyException {
			ECKey ecKey = ECKey.fromPublicOnly(Utils.HEX.decode(pubKey));
			ECPoint pubKeyPoint = ecKey.getPubKeyPoint();
			Address to = ecKey.toAddress(params);
			
			Wallet.SendRequest req = Wallet.SendRequest.to(to, SHARING_COST);
			req.shuffleOutputs = false;
			wallet.completeTx(req);

			TransactionInput in = req.tx.getInput(0);
			BigInteger privKey = TransactionUtils.getPrivateKey(wallet, in);
			System.out.println("From pubKey: " + Utils.HEX.encode(ECKey.fromPublicOnly(in.getScriptSig().getPubKey()).decompress().getPubKeyPoint().getEncoded(true)));
			System.out.println("To pubKey: " + pubKey);
			System.out.println("To address: " + ecKey.toAddress(params));

	        SendResult result = new SendResult();
	        result.tx = req.tx;
			
	        byte[] secret = ECUtils.getSharedSecret(pubKeyPoint, privKey);
			KeyParameter iKey = new KeyParameter(secret);
			IntermediateFile permission = new IntermediateFile(fileHashStr, encryptionKeyStr);
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
			wallet.sendCoins(Wallet.SendRequest.forTx(copy));
			
			ShareResult shareResult = new ShareResult(result, permission);
			this.results.put(pubKey, shareResult);
		}
		
		public ShareRequest build() throws InsufficientMoneyException {
			this.results = new HashMap<String, ShareResult>();

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
					ECKey newKey = wallet.freshKey(KeyPurpose.RECEIVE_FUNDS);
					shareWith(ECUtils.toPubKeyString(newKey));
				}
			}
				
			for (String pubKey: shareWith) {
				doShareWith(pubKey);
			}
			
			return new ShareRequest(this);
		}
	}

}
