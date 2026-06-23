package com.duoc.empresa_transportista_efs.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.duoc.empresa_transportista_efs.dto.GuiaCreadaResponse;
import com.duoc.empresa_transportista_efs.exception.GuiaYaGeneradaException;
import com.duoc.empresa_transportista_efs.model.Pedido;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoGuiaService {

	private final PedidoService pedidoService;
	private final GuiaGeneradorService guiaGeneradorService;
	private final GuiaDespachoService guiaDespachoService;
	private final EfsService efsService;
	private final AwsS3Service awsS3Service;

	@Value("${aws.s3.bucket}")
	private String bucket;

	public GuiaCreadaResponse generarGuia(String pedidoId) {
		Pedido pedido = pedidoService.obtenerPedido(pedidoId);

		if (pedido.isGuiaGenerada()) {
			throw new GuiaYaGeneradaException(pedidoId);
		}

		String nombreGuia = guiaGeneradorService.nombreGuia(pedidoId);
		String key = guiaDespachoService.buildKey(pedido.getFecha(), pedido.getTransportista(), nombreGuia);
		byte[] pdf = guiaGeneradorService.generarPdf(pedido);

		try {
			efsService.saveBytes(key, pdf);
			awsS3Service.uploadBytes(bucket, key, pdf, "application/pdf");
		} catch (Exception e) {
			log.error("Error al guardar guia del pedido {}: {}", pedidoId, e.getMessage(), e);
			throw new IllegalStateException("No se pudo guardar la guia generada: " + e.getMessage(), e);
		}

		pedidoService.marcarGuiaGenerada(pedido, key, guiaDespachoService.obtenerNombreArchivo(key));

		return GuiaCreadaResponse.builder()
				.key(key)
				.fecha(pedido.getFecha())
				.transportista(pedido.getTransportista())
				.nombreGuia(guiaDespachoService.obtenerNombreArchivo(key))
				.mensaje("Guia generada y almacenada en EFS y S3")
				.build();
	}
}
