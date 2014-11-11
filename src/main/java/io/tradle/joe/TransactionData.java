package io.tradle.joe;

import java.io.Serializable;
import java.util.Arrays;

public class TransactionData implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private static final byte[] prefixBytes = Joe.JOE.getDataPrefix();
	private static final int prefixLength = prefixBytes == null ? 0 : prefixBytes.length;
	private static final Config config = Joe.JOE.config();

	private TransactionDataType type;
	private byte[] data;
	
	private TransactionData() {}
	
	public TransactionData(TransactionDataType type, byte[] rawData) {
		this.type = type;
		this.data = rawData;
	}
	
	public byte[] data() {
		return data;
	}
	
	public TransactionDataType type() {
		return type;
	}
	
	public byte[] serialize() {
		int totalLength = prefixBytes.length + data.length + 1; // type marker byte
		if (totalLength > config.maxDataBytes())
			throw new IllegalArgumentException("Data too long by " + (totalLength - config.maxDataBytes()) + " bytes");
		
    	byte[] serialized = new byte[Math.min(config.maxDataBytes(), totalLength)];
    	if (prefixBytes != null)
    		System.arraycopy(prefixBytes, 0, serialized, 0, prefixLength);
    	
    	serialized[prefixLength] = type.marker();
    	
    	System.arraycopy(data, 0, serialized, prefixLength + 1, data.length);
    	return serialized;
	}
	
	public static TransactionData deserialize(byte[] serialized) {
		TransactionData data = new TransactionData();
		data.type = TransactionDataType.getType(serialized[prefixLength]);
		data.data = Arrays.copyOfRange(serialized, prefixLength + 1, serialized.length);
		return data;
	}	
}
