package io.tradle.joe.exceptions;

public class InvalidWebHookRequestException extends BadRequestException {

	public InvalidWebHookRequestException() {
		super();
	}
	
	public InvalidWebHookRequestException(String msg) {
		super(msg);
	}

	public InvalidWebHookRequestException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
}
