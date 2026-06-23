package com.duoc.empresa_transportista_efs.exception;

public class S3ObjectNotFoundException extends RuntimeException {

	public S3ObjectNotFoundException(String key, String bucket) {
		super("El objeto '" + key + "' no existe en el bucket '" + bucket + "'");
	}

	public S3ObjectNotFoundException(String key, String bucket, Throwable cause) {
		super("El objeto '" + key + "' no existe en el bucket '" + bucket + "'", cause);
	}
}
