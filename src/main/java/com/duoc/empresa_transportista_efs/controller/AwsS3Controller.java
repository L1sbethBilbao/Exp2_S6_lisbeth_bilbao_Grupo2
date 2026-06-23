package com.duoc.empresa_transportista_efs.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.duoc.empresa_transportista_efs.dto.GuiaConsultaResponse;
import com.duoc.empresa_transportista_efs.dto.S3ObjectDto;
import com.duoc.empresa_transportista_efs.service.AwsS3Service;
import com.duoc.empresa_transportista_efs.service.EfsService;
import com.duoc.empresa_transportista_efs.service.GuiaDespachoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/s3")
@RequiredArgsConstructor
public class AwsS3Controller {

	@Autowired
	private AwsS3Service awsS3Service;

	@Autowired
	private EfsService efsService;

	@Autowired
	private GuiaDespachoService guiaDespachoService;

	/**
	 * Lista todos los objetos en un bucket de S3
	 * 
	 * @param bucket Nombre del bucket
	 * @return Lista de objetos con sus metadatos
	 */
	@GetMapping("/{bucket}/objects")
	public ResponseEntity<List<S3ObjectDto>> listObjects(@PathVariable String bucket) {

		List<S3ObjectDto> dtoList = awsS3Service.listObjects(bucket);
		return ResponseEntity.ok(dtoList);
	}

	/**
	 * Consulta guias de despacho por fecha y transportista (actividad empresa transportista)
	 * 
	 * @param bucket        Nombre del bucket
	 * @param fecha         Fecha de busqueda (obligatoria)
	 * @param transportista Nombre del transportista (opcional)
	 * @return Lista de guias con total y metadatos
	 */
	@GetMapping("/{bucket}/consulta")
	public ResponseEntity<GuiaConsultaResponse> consultarGuias(@PathVariable String bucket,
			@RequestParam String fecha, @RequestParam(required = false) String transportista) {

		GuiaConsultaResponse response = guiaDespachoService.consultarGuias(bucket, fecha, transportista);
		return ResponseEntity.ok(response);
	}

	/**
	 * Descarga un objeto de S3 como array de bytes
	 * 
	 * @param bucket        Nombre del bucket
	 * @param key           Clave del objeto (modo profesor)
	 * @param fecha         Fecha de la guia (modo actividad)
	 * @param transportista Nombre del transportista (modo actividad)
	 * @param nombreGuia    Nombre de la guia (modo actividad)
	 * @return Archivo descargado como bytes
	 */
	@GetMapping("/{bucket}/object")
	public ResponseEntity<byte[]> downloadObject(@PathVariable String bucket,
			@RequestParam(required = false) String key, @RequestParam(required = false) String fecha,
			@RequestParam(required = false) String transportista, @RequestParam(required = false) String nombreGuia) {

		String resolvedKey = guiaDespachoService.resolveKey(key, fecha, transportista, nombreGuia);
		byte[] fileBytes = awsS3Service.downloadAsBytes(bucket, resolvedKey);

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resolvedKey + "\"")
				.contentType(MediaType.APPLICATION_OCTET_STREAM).body(fileBytes);
	}

	/**
	 * Sube un archivo a S3 y lo almacena en EFS
	 * 
	 * @param bucket        Nombre del bucket
	 * @param key           Clave del objeto (modo profesor)
	 * @param fecha         Fecha de la guia (modo actividad)
	 * @param transportista Nombre del transportista (modo actividad)
	 * @param nombreGuia    Nombre de la guia (modo actividad)
	 * @param file          Archivo a subir
	 * @return Respuesta de exito
	 */
	@PostMapping("/{bucket}/object")
	public ResponseEntity<Void> uploadObject(@PathVariable String bucket, @RequestParam(required = false) String key,
			@RequestParam(required = false) String fecha, @RequestParam(required = false) String transportista,
			@RequestParam(required = false) String nombreGuia, @RequestParam("file") MultipartFile file) {

		try {

			String resolvedKey = guiaDespachoService.resolveKey(key, fecha, transportista, nombreGuia);

			efsService.saveToEfs(resolvedKey, file);

			awsS3Service.upload(bucket, resolvedKey, file);

			return ResponseEntity.status(HttpStatus.CREATED).build();
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().build();
		}
	}

	/**
	 * Actualiza una guia existente en EFS y S3 (actividad empresa transportista)
	 * 
	 * @param bucket        Nombre del bucket
	 * @param key           Clave del objeto (modo profesor)
	 * @param fecha         Fecha de la guia (modo actividad)
	 * @param transportista Nombre del transportista (modo actividad)
	 * @param nombreGuia    Nombre de la guia (modo actividad)
	 * @param file          Archivo a subir
	 * @return Respuesta de exito
	 */
	@PutMapping("/{bucket}/object")
	public ResponseEntity<Void> updateObject(@PathVariable String bucket, @RequestParam(required = false) String key,
			@RequestParam(required = false) String fecha, @RequestParam(required = false) String transportista,
			@RequestParam(required = false) String nombreGuia, @RequestParam("file") MultipartFile file) {

		try {

			String oldKey = guiaDespachoService.resolveKey(key, fecha, transportista, nombreGuia);
			String newKey = guiaDespachoService.buildActualizadoKey(fecha, transportista, nombreGuia);

			efsService.saveToEfs(newKey, file);
			awsS3Service.upload(bucket, newKey, file);
			efsService.deleteFile(oldKey);
			awsS3Service.deleteObject(bucket, oldKey);

			return ResponseEntity.ok().build();
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().build();
		}
	}

	/**
	 * Mueve un objeto dentro del mismo bucket
	 * 
	 * @param bucket    Nombre del bucket
	 * @param sourceKey Clave del objeto origen
	 * @param destKey   Clave del objeto destino
	 * @return Respuesta de exito
	 */
	@PostMapping("/{bucket}/move")
	public ResponseEntity<Void> moveObject(@PathVariable String bucket, @RequestParam String sourceKey,
			@RequestParam String destKey) {

		awsS3Service.moveObject(bucket, sourceKey, destKey);
		return ResponseEntity.ok().build();
	}

	/**
	 * Elimina un objeto de EFS y S3
	 * 
	 * @param bucket        Nombre del bucket
	 * @param key           Clave del objeto (modo profesor)
	 * @param fecha         Fecha de la guia (modo actividad)
	 * @param transportista Nombre del transportista (modo actividad)
	 * @param nombreGuia    Nombre de la guia (modo actividad)
	 * @return Respuesta sin contenido
	 */
	@DeleteMapping("/{bucket}/object")
	public ResponseEntity<Void> deleteObject(@PathVariable String bucket, @RequestParam(required = false) String key,
			@RequestParam(required = false) String fecha, @RequestParam(required = false) String transportista,
			@RequestParam(required = false) String nombreGuia) {

		try {

			String resolvedKey = guiaDespachoService.resolveKey(key, fecha, transportista, nombreGuia);
			log.info("DELETE guia — key resuelta: {}", resolvedKey);
			efsService.deleteFile(resolvedKey);
			awsS3Service.deleteObject(bucket, resolvedKey);
			return ResponseEntity.noContent().build();
		} catch (Exception e) {
			log.error("Error en DELETE guia (fecha={}, transportista={}, nombreGuia={}): {}",
					fecha, transportista, nombreGuia, e.getMessage(), e);
			return ResponseEntity.internalServerError().build();
		}
	}
}
