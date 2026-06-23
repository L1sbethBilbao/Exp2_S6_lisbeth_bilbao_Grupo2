package com.duoc.empresa_transportista_efs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GuiaCreadaResponse {

	private String key;
	private String fecha;
	private String transportista;
	private String nombreGuia;
	private String mensaje;
}
