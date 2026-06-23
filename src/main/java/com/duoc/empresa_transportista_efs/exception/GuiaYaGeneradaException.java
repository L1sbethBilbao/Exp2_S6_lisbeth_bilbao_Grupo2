package com.duoc.empresa_transportista_efs.exception;

public class GuiaYaGeneradaException extends RuntimeException {

	public GuiaYaGeneradaException(String pedidoId) {
		super("El pedido " + pedidoId + " ya tiene una guia generada");
	}
}
