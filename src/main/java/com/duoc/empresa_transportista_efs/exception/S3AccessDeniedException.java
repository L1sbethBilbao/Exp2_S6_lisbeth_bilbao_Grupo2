package com.duoc.empresa_transportista_efs.exception;

public class S3AccessDeniedException extends RuntimeException {

	public S3AccessDeniedException(String operation) {
		super("Acceso denegado al intentar realizar la operación: " + operation);
	}

	public S3AccessDeniedException(String operation, Throwable cause) {
		super("Acceso denegado al intentar realizar la operación: " + operation, cause);
	}
}
