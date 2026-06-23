package com.duoc.empresa_transportista_efs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GuiaMetadataDto {

	private String key;
	private Long size;
	private String lastModified;
}
