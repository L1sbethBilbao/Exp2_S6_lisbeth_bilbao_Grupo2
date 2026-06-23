package com.duoc.empresa_transportista_efs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PedidoResponse {

	private String id;
	private String cliente;
	private String direccion;
	private String descripcion;
	private String transportista;
	private String fecha;
	private boolean guiaGenerada;
	private String s3Key;
	private String nombreGuia;
}
