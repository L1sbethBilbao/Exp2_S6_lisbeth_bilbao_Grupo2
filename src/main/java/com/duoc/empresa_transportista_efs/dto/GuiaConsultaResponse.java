package com.duoc.empresa_transportista_efs.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GuiaConsultaResponse {

	private int total;
	private String fecha;
	private String transportista;
	private List<GuiaMetadataDto> guias;
}
