package com.duoc.empresa_transportista_efs.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PedidoRequest {

	@NotBlank(message = "El cliente es obligatorio")
	private String cliente;

	@NotBlank(message = "La direccion es obligatoria")
	private String direccion;

	@NotBlank(message = "La descripcion es obligatoria")
	private String descripcion;

	@NotBlank(message = "El transportista es obligatorio")
	private String transportista;

	@NotBlank(message = "La fecha es obligatoria")
	private String fecha;
}
