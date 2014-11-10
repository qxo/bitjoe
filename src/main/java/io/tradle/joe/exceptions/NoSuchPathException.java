package io.tradle.joe.exceptions;

public class NoSuchPathException extends BadRequestException {
	public NoSuchPathException() {
		super();
	}
	
	public NoSuchPathException(String msg) {
		super(msg);
	}

	public NoSuchPathException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
