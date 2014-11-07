package io.tradle.joe.utils;

import io.tradle.joe.Config;
import io.tradle.joe.Joe;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionBag;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.wallet.RedeemData;

public class TransactionUtils {

	public static void addDataToTransaction(Transaction tx, byte[] data) {
		Config config = Joe.JOE.config();
    	byte[] prefixBytes = Joe.JOE.getDataPrefix();
    	if (prefixBytes != null) {
	    	int totalLength = prefixBytes.length + data.length;
	    	if (totalLength > config.maxDataBytes())
	    		throw new IllegalArgumentException("Data too long by " + (config.maxDataBytes() - prefixBytes.length - data.length) + " bytes");
	
	    	byte[] appAndData = new byte[Math.min(config.maxDataBytes(), totalLength)];
	    	System.arraycopy(prefixBytes, 0, appAndData, 0, prefixBytes.length);
	    	System.arraycopy(data, 0, appAndData, prefixBytes.length, data.length);
	    	data = appAndData;
    	}

		tx.addOutput(new TransactionOutput(tx.getParams(), tx, Coin.ZERO, metadataToScript(data)));
	}

	public static String transactionDataToString(byte[] data) {
		return Base58.encode(data);
	}
	
	public static byte[] getDataFromTransaction(Transaction tx) {
		TransactionOutput opReturnOutput = getOpReturnOutput(tx);
		if (opReturnOutput == null)
			return null;
		
		byte[] data = scriptToMetadata(opReturnOutput.getScriptBytes());
    	byte[] prefixBytes = Joe.JOE.getDataPrefix();
		if (prefixBytes != null)
			data = Arrays.copyOfRange(data, prefixBytes.length, data.length);
		
		return data;
	}

	public static TransactionOutput getOpReturnOutput(Transaction tx) {
		List<TransactionOutput> txOuts = tx.getOutputs();
		for (TransactionOutput t: txOuts) {
			if (t.getValue().isZero()) {
				byte[] scriptBytes = t.getScriptBytes();
				if (scriptBytes != null)
					return t;
			}
		}
		
		return null;
	}		
	
	public static List<TransactionOutput> getReceived(TransactionBag tBag, Transaction tx) {
		List<TransactionOutput> mine = new ArrayList<TransactionOutput>();
        for (TransactionOutput output : tx.getOutputs()) {
            if (output.isMine(tBag) && !output.getValue().isZero())
            	mine.add(output);
        }
        
        return mine;
	}

	public static List<TransactionOutput> getSent(TransactionBag tBag, Transaction tx) {
		List<TransactionOutput> notMine = new ArrayList<TransactionOutput>();
        for (TransactionOutput output : tx.getOutputs()) {
            if (!output.isMine(tBag) && !output.getValue().isZero())
            	notMine.add(output);
        }
        
        return notMine;
	}

	public static List<TransactionInput> getSentByMe(TransactionBag tBag, Transaction tx) {
		List<TransactionInput> mine = new ArrayList<TransactionInput>();
        for (TransactionInput input : tx.getInputs()) {
        	TransactionOutput out = input.getConnectedOutput();
        	if (out != null && out.isMine(tBag))
            	mine.add(input);
        }
        
        return mine;
	}
	
	/**
	 * From CoinSparkBase: https://github.com/coinspark/libraries/blob/master/java/src/main/java/org/coinspark/protocol/CoinSparkBase.java
     * 
     * Converts CoinSpark metadata (or other data) into an OP_RETURN bitcoin tx output script.
     * 
     * @param metadata metadata  Raw binary metadata to be converted.
     * @return string | null The OP_RETURN bitcoin tx output script as hexadecimal, null if we failed.
	 */
	public static byte[] metadataToScript(byte [] metadata) {
        byte [] scriptPubKey;
        if ( (metadata.length <= 75))// && (scriptPubKeyMaxLen>=scriptLength) ) {
        {
            int scriptRawLen = metadata.length+2;

            scriptPubKey=new byte[scriptRawLen];
            
            scriptPubKey[0]=0x6a;
            scriptPubKey[1] = (byte)metadata.length;
            
            System.arraycopy(metadata, 0, scriptPubKey, 2, metadata.length);
            return scriptPubKey;
        }

        return null;
    }

	/**
     * Extracts OP_RETURN metadata (not necessarily CoinSpark data) from a bitcoin tx output script.
     * 
     * @param scriptPubKey Output script as hexadecimal.
     * @return byte [] | null Raw binary embedded metadata if found, null otherwise. 
     */
    
    public static byte [] scriptToMetadata(String scriptPubKey)
    {
        return scriptToMetadata(hexToByte(scriptPubKey));
    }

    /**
     * Extracts OP_RETURN metadata (not necessarily CoinSpark data) from a bitcoin tx output script.
     * 
     * @param scriptPubKey Output script as raw binary data.
     * @return byte [] | null Raw binary embedded metadata if found, null otherwise. 
     */
    
    public static byte [] scriptToMetadata(byte[] scriptPubKey)
    {
        if(scriptPubKey == null)
            return null;
        
        int scriptPubKeyLen = scriptPubKey.length;
        int metadataLength = scriptPubKeyLen-2;  // Skip the signature

        if ( (scriptPubKeyLen>2) && (scriptPubKey[0]==0x6a) &&
             (scriptPubKey[1]>0) && (scriptPubKey[1]<=75) && (scriptPubKey[1] == metadataLength))
        {
            return Arrays.copyOfRange(scriptPubKey, 2, scriptPubKeyLen);
        }

        return null;
    }

    public static byte[] hexToByte(String str) {
        if (str == null)
            return null;

        byte[] bytes = new byte[str.length() / 2];
        for (int i = 0; i < bytes.length; i++)
        {
            bytes[i] = (byte) Integer
                    .parseInt(str.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }

    /**
     * <p>Returns true if the list of transaction outputs, whether spent or unspent, match a wallet by address or that are
     * watched by a wallet, i.e., transaction outputs whose script's address is controlled by the wallet and transaction
     * outputs whose script is watched by the wallet.</p>
     *
     * @param transactionBag The wallet that controls addresses and watches scripts.
     */
	public static boolean hasWalletOutputs(Transaction tx, Wallet wallet) {
		List<TransactionOutput> outs = tx.getOutputs();
		for (TransactionOutput o: outs) {
			if (o.isMineOrWatched(wallet))
				return true;
		}
		
		return false;
	}
	
	public static BigInteger getPrivateKey(Wallet wallet, TransactionInput input) {
//		KeyBag keyBag = new DecryptingKeyBag(wallet, null);
		RedeemData redeemData = input.getConnectedRedeemData(wallet);
        return redeemData.getFullKey().getPrivKey();
	}

}
