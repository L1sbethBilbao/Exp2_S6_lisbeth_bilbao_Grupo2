package com.duoc.empresa_transportista_efs.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duoc.empresa_transportista_efs.dto.GuiaCreadaResponse;
import com.duoc.empresa_transportista_efs.dto.PedidoRequest;
import com.duoc.empresa_transportista_efs.dto.PedidoResponse;
import com.duoc.empresa_transportista_efs.service.PedidoGuiaService;
import com.duoc.empresa_transportista_efs.service.PedidoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

	private final PedidoService pedidoService;
	private final PedidoGuiaService pedidoGuiaService;

	@PostMapping
	public ResponseEntity<PedidoResponse> crear(@Valid @RequestBody PedidoRequest request) {
		PedidoResponse response = pedidoService.crear(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	public ResponseEntity<List<PedidoResponse>> listar() {
		return ResponseEntity.ok(pedidoService.listar());
	}

	@GetMapping("/{id}")
	public ResponseEntity<PedidoResponse> obtener(@PathVariable String id) {
		return ResponseEntity.ok(pedidoService.obtener(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<PedidoResponse> actualizar(@PathVariable String id,
			@Valid @RequestBody PedidoRequest request) {
		return ResponseEntity.ok(pedidoService.actualizar(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> eliminar(@PathVariable String id) {
		pedidoService.eliminar(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/generar-guia")
	public ResponseEntity<GuiaCreadaResponse> generarGuia(@PathVariable String id) {
		GuiaCreadaResponse response = pedidoGuiaService.generarGuia(id);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}
