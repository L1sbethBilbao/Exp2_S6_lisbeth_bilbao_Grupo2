package com.duoc.empresa_transportista_efs.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import com.duoc.empresa_transportista_efs.dto.PedidoRequest;
import com.duoc.empresa_transportista_efs.dto.PedidoResponse;
import com.duoc.empresa_transportista_efs.exception.ResourceNotFoundException;
import com.duoc.empresa_transportista_efs.model.Pedido;

@Service
public class PedidoService {

	private final Map<String, Pedido> pedidos = new ConcurrentHashMap<>();
	private final AtomicInteger contador = new AtomicInteger(0);

	public PedidoResponse crear(PedidoRequest request) {
		String id = generarId();
		Pedido pedido = Pedido.builder()
				.id(id)
				.cliente(request.getCliente().trim())
				.direccion(request.getDireccion().trim())
				.descripcion(request.getDescripcion().trim())
				.transportista(request.getTransportista().trim())
				.fecha(request.getFecha().trim())
				.guiaGenerada(false)
				.build();

		pedidos.put(id, pedido);
		return toResponse(pedido);
	}

	public List<PedidoResponse> listar() {
		return pedidos.values().stream().map(this::toResponse).toList();
	}

	public PedidoResponse obtener(String id) {
		return toResponse(obtenerPedido(id));
	}

	public Pedido obtenerPedido(String id) {
		Pedido pedido = pedidos.get(id);
		if (pedido == null) {
			throw new ResourceNotFoundException("No existe el pedido con id: " + id);
		}
		return pedido;
	}

	public PedidoResponse actualizar(String id, PedidoRequest request) {
		Pedido pedido = obtenerPedido(id);
		pedido.setCliente(request.getCliente().trim());
		pedido.setDireccion(request.getDireccion().trim());
		pedido.setDescripcion(request.getDescripcion().trim());
		pedido.setTransportista(request.getTransportista().trim());
		pedido.setFecha(request.getFecha().trim());
		return toResponse(pedido);
	}

	public void eliminar(String id) {
		if (pedidos.remove(id) == null) {
			throw new ResourceNotFoundException("No existe el pedido con id: " + id);
		}
		if (pedidos.isEmpty()) {
			contador.set(0);
		}
	}

	public void marcarGuiaGenerada(Pedido pedido, String s3Key, String nombreGuia) {
		pedido.setGuiaGenerada(true);
		pedido.setS3Key(s3Key);
		pedido.setNombreGuia(nombreGuia);
	}

	private String generarId() {
		if (pedidos.isEmpty()) {
			contador.set(0);
		}
		return String.format("PED-%03d", contador.incrementAndGet());
	}

	private PedidoResponse toResponse(Pedido pedido) {
		return PedidoResponse.builder()
				.id(pedido.getId())
				.cliente(pedido.getCliente())
				.direccion(pedido.getDireccion())
				.descripcion(pedido.getDescripcion())
				.transportista(pedido.getTransportista())
				.fecha(pedido.getFecha())
				.guiaGenerada(pedido.isGuiaGenerada())
				.s3Key(pedido.getS3Key())
				.nombreGuia(pedido.getNombreGuia())
				.build();
	}
}
