package io.tradle.joe.exceptions;

public class InvalidTransactionRequestException extends BadRequestException {

	public InvalidTransactionRequestException() {
		super();
	}
	
	public InvalidTransactionRequestException(String msg) {
		super(msg);
	}

	public InvalidTransactionRequestException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
}
