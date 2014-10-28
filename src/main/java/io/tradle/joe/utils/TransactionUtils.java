package io.tradle.joe.utils;

import io.tradle.joe.Joe;

import java.util.Arrays;
import java.util.List;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Wallet;

public class TransactionUtils {

	public static void addDataToTransaction(Transaction tx, byte[] data) {
		tx.addOutput(new TransactionOutput(tx.getParams(), tx, Coin.ZERO, metadataToScript(data)));
	}

	public static String transactionDataToString(byte[] data) {
		return Base58.encode(data);
	}
	
	public static byte[] getDataFromTransaction(Transaction tx) {
		return getDataFromTransaction(tx, false);
	}
	
	public static byte[] getDataFromTransaction(Transaction tx, boolean removePrefix) {
		TransactionOutput opReturnOutput = getOpReturnOutput(tx);
		byte[] data = opReturnOutput == null ? null : scriptToMetadata(opReturnOutput.getScriptBytes());
		if (removePrefix) {
			byte[] prefixBytes = Joe.JOE.getDataPrefix();
			if (prefixBytes != null)
				data = Arrays.copyOfRange(data, prefixBytes.length, data.length);
		}
		
		return data;
	}

	public static TransactionOutput getOpReturnOutput(Transaction tx) {
		List<TransactionOutput> txOuts = tx.getOutputs();
		byte[] prefix = Joe.JOE.getDataPrefix();
		for (TransactionOutput t: txOuts) {
			if (t.getValue().isZero()) {
				byte[] scriptBytes = t.getScriptBytes();
				if (scriptBytes != null && Utils.arrayStartsWith(scriptBytes, prefix))
					return t;
			}
		}
		
		return null;
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
    
    protected static byte[] hexToByte(String str)
    {
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
}
