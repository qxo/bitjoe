package io.tradle.joe;

public enum TransactionDataType {

	CLEARTEXT_STORE,
	ENCRYPTED_SHARE;
	
	public byte marker() {
		return (byte) this.ordinal();
	}

	public static TransactionDataType getType(byte marker) {
		for (TransactionDataType type: values()) {
			if (type.marker() == marker)
				return type;
		}
		
		return null;
	}
}
