package io.tradle.joe.exceptions;

public class NotEnoughFundsException extends RuntimeException {
	
	public NotEnoughFundsException() {
		super();
	}
	
	public NotEnoughFundsException(String msg) {
		super(msg);
	}

	public NotEnoughFundsException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
