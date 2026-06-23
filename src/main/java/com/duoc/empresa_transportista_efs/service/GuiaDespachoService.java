package com.duoc.empresa_transportista_efs.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.duoc.empresa_transportista_efs.dto.GuiaConsultaResponse;
import com.duoc.empresa_transportista_efs.dto.GuiaMetadataDto;
import com.duoc.empresa_transportista_efs.exception.InvalidFileException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuiaDespachoService {

	private final AwsS3Service awsS3Service;

	/**
	 * Consulta guias de despacho por fecha y transportista.
	 *
	 * @param bucket         Nombre del bucket S3
	 * @param fecha          Fecha de busqueda (obligatoria)
	 * @param transportista  Nombre del transportista (opcional)
	 * @return Lista de guias con sus metadatos
	 */
	public GuiaConsultaResponse consultarGuias(String bucket, String fecha, String transportista) {
		validarFecha(fecha);

		String prefix = buildListPrefix(fecha, transportista);
		List<GuiaMetadataDto> guias = awsS3Service.listObjects(bucket).stream()
				.filter(obj -> obj.getKey().startsWith(prefix))
				.map(obj -> new GuiaMetadataDto(obj.getKey(), obj.getSize(), obj.getLastModified()))
				.collect(Collectors.toList());

		return GuiaConsultaResponse.builder()
				.total(guias.size())
				.fecha(fecha.trim())
				.transportista(transportista != null ? transportista.trim() : "")
				.guias(guias)
				.build();
	}

	/**
	 * Resuelve la clave S3/EFS de una guia.
	 * Modo profesor: key = pdfs/testEFS1.pdf
	 * Modo actividad: fecha/transportista/nombreGuia.pdf
	 *
	 * @param keyParam       Clave completa opcional
	 * @param fecha          Fecha de la guia
	 * @param transportista  Nombre del transportista
	 * @param nombreGuia     Nombre del archivo de la guia
	 * @return Clave normalizada del objeto
	 */
	public String resolveKey(String keyParam, String fecha, String transportista, String nombreGuia) {
		if (keyParam != null && !keyParam.isBlank()) {
			return normalizarKey(keyParam);
		}
		validarParametros(fecha, transportista, nombreGuia);
		return buildKey(fecha, transportista, nombreGuia);
	}

	/**
	 * Construye la clave S3/EFS con el formato fecha/transportista/nombreGuia.pdf.
	 *
	 * @param fecha          Fecha de la guia
	 * @param transportista  Nombre del transportista
	 * @param nombreGuia     Nombre del archivo de la guia
	 * @return Clave construida
	 */
	public String buildKey(String fecha, String transportista, String nombreGuia) {
		return fecha.trim() + "/" + transportista.trim() + "/" + normalizarNombreGuia(nombreGuia);
	}

	public String buildActualizadoKey(String fecha, String transportista, String nombreGuia) {
		validarParametros(fecha, transportista, nombreGuia);
		String nombreActualizado = normalizarNombreGuia(nombreGuia).replace(".pdf", "") + "-actualizado";
		return buildKey(fecha, transportista, nombreActualizado);
	}

	/**
	 * Obtiene el nombre del archivo a partir de la clave S3/EFS.
	 *
	 * @param key Clave del objeto
	 * @return Nombre del archivo (ultimo segmento de la ruta)
	 */
	public String obtenerNombreArchivo(String key) {
		int lastSlash = key.lastIndexOf('/');
		return lastSlash >= 0 ? key.substring(lastSlash + 1) : key;
	}

	private String normalizarNombreGuia(String nombreGuia) {
		if (nombreGuia == null || nombreGuia.isBlank()) {
			throw new InvalidFileException("El nombre de la guia es obligatorio");
		}
		String nombre = nombreGuia.trim();
		if (!nombre.toLowerCase().endsWith(".pdf")) {
			nombre = nombre + ".pdf";
		}
		return nombre;
	}

	private String normalizarKey(String key) {
		String normalized = key.trim().replace('\\', '/');
		if (!normalized.toLowerCase().endsWith(".pdf")) {
			normalized = normalized + ".pdf";
		}
		return normalized;
	}

	private String buildListPrefix(String fecha, String transportista) {
		if (transportista == null || transportista.isBlank()) {
			return fecha.trim() + "/";
		}
		return fecha.trim() + "/" + transportista.trim() + "/";
	}

	private void validarFecha(String fecha) {
		if (fecha == null || fecha.isBlank()) {
			throw new InvalidFileException("La fecha es obligatoria");
		}
	}

	private void validarParametros(String fecha, String transportista, String nombreGuia) {
		validarFecha(fecha);
		if (transportista == null || transportista.isBlank()) {
			throw new InvalidFileException("El transportista es obligatorio");
		}
		if (nombreGuia == null || nombreGuia.isBlank()) {
			throw new InvalidFileException("El nombre de la guia es obligatorio");
		}
	}
}
