package com.duoc.empresa_transportista_efs.service;

import java.io.ByteArrayOutputStream;

import org.springframework.stereotype.Service;

import com.duoc.empresa_transportista_efs.model.Pedido;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class GuiaGeneradorService {

	private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
	private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 12);

	public byte[] generarPdf(Pedido pedido) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			Document document = new Document();
			PdfWriter.getInstance(document, output);
			document.open();

			document.add(new Paragraph("GUIA DE DESPACHO", TITULO));
			document.add(new Paragraph(" "));
			document.add(new Paragraph("Pedido: " + pedido.getId(), NORMAL));
			document.add(new Paragraph("Cliente: " + pedido.getCliente(), NORMAL));
			document.add(new Paragraph("Direccion: " + pedido.getDireccion(), NORMAL));
			document.add(new Paragraph("Descripcion: " + pedido.getDescripcion(), NORMAL));
			document.add(new Paragraph("Transportista: " + pedido.getTransportista(), NORMAL));
			document.add(new Paragraph("Fecha: " + pedido.getFecha(), NORMAL));

			document.close();
			return output.toByteArray();
		} catch (DocumentException e) {
			throw new IllegalStateException("Error al generar el PDF de la guia", e);
		}
	}

	public String nombreGuia(String pedidoId) {
		return "guia-" + pedidoId;
	}
}
