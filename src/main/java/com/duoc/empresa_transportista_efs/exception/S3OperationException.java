package com.duoc.empresa_transportista_efs.exception;

public class S3OperationException extends RuntimeException {

	public S3OperationException(String message) {
		super(message);
	}

	public S3OperationException(String message, Throwable cause) {
		super(message, cause);
	}
}
