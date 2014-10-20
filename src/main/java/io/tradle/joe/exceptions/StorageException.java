package io.tradle.joe.exceptions;

public class StorageException extends RuntimeException {

	public StorageException() {
		super();
	}
	
	public StorageException(String msg) {
		super(msg);
	}

	public StorageException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
