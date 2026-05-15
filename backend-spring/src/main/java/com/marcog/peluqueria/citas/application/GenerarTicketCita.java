package com.marcog.peluqueria.citas.application;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.marcog.peluqueria.citas.domain.Cita;
import com.marcog.peluqueria.citas.domain.CitaRepository;
import com.marcog.peluqueria.servicios.domain.Servicio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * HU13: Servicio para Generar el Ticket/Factura en formato PDF.
 * Utiliza iText 5 para construir un informe detallado de la Cita de un cliente.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GenerarTicketCita {

    private final CitaRepository citaRepository;

    public byte[] generarTicket(UUID citaId) {
        log.info("Generando Ticket PDF para la Cita ID: {}", citaId);

        // 1. Buscamos la cita real en la DB
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada para generar ticket."));

        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Formatos de Letra (Fonts)
            Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, BaseColor.DARK_GRAY);
            Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK);
            Font headerTableFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE);

            // 2. Cabecera (Logo TFG simulado y Título)
            Paragraph titulo = new Paragraph("TICKET DE SERVICIOS - PELUQUERÍA MARCO G", tituloFont);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(20);
            document.add(titulo);

            // 3. Detalles de Cliente y Fecha
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String fechaAte = cita.getFechaHora().format(formatter);
            String nombreCliente = cita.getCliente() != null ? cita.getCliente().getNombre() : "Cliente Desconocido";
            String profesional = cita.getPeluquero() != null ? cita.getPeluquero().getNombre() : "Staff";

            document.add(new Paragraph("Fecha de emisión: " + fechaAte, infoFont));
            document.add(new Paragraph("Cliente atendido: " + nombreCliente, infoFont));
            document.add(new Paragraph("Profesional a cargo: " + profesional, infoFont));
            document.add(new Paragraph("Estado: " + cita.getEstado().name(), infoFont));
            document.add(Chunk.NEWLINE);

            // 4. Tabla de Conceptos (Servicios)
            PdfPTable table = new PdfPTable(3); // 3 Columnas
            table.setWidthPercentage(100);

            // Definiendo anchos de columna relativos (Ej: La descripción es más grande)
            float[] columnWidths = { 2f, 4f, 2f };
            table.setWidths(columnWidths);

            // Cabeceras de Tabla
            Stream.of("CANT.", "CONCEPTO - SERVICIO", "PRECIO (€)")
                    .forEach(columnTitle -> {
                        PdfPCell header = new PdfPCell();
                        header.setBackgroundColor(new BaseColor(107, 33, 168)); // Color Morado de nuestro TFG (Purple
                                                                                // 700)
                        header.setBorderWidth(1);
                        header.setPhrase(new Paragraph(columnTitle, headerTableFont));
                        header.setHorizontalAlignment(Element.ALIGN_CENTER);
                        header.setPaddingBottom(8);
                        table.addCell(header);
                    });

            // 5. Añadimos el servicio (En este TFG las citas de prueba pueden tener
            // servicio en descripción manual, pero lo simulamos como factura)
            BigDecimal totalEsperado = BigDecimal.ZERO;

            if (cita.getServicios() != null && !cita.getServicios().isEmpty()) {
                for (Servicio s : cita.getServicios()) {
                    String servicioNombre = s.getNombre();
                    BigDecimal precio = precioVenta(s);
                    totalEsperado = totalEsperado.add(precio);

                    PdfPCell cellCant = new PdfPCell(new Phrase("1"));
                    cellCant.setHorizontalAlignment(Element.ALIGN_CENTER);

                    PdfPCell cellDesc = new PdfPCell(new Phrase(servicioNombre));

                    PdfPCell cellPrecio = new PdfPCell(new Phrase(precio.toString() + " €"));
                    cellPrecio.setHorizontalAlignment(Element.ALIGN_RIGHT);

                    table.addCell(cellCant);
                    table.addCell(cellDesc);
                    table.addCell(cellPrecio);
                }
            } else {
                // Fila por defecto si no hay servicios asignados para propósitos de
                // demostración.
                PdfPCell cellCant = new PdfPCell(new Phrase("1"));
                cellCant.setHorizontalAlignment(Element.ALIGN_CENTER);

                PdfPCell cellDesc = new PdfPCell(new Phrase("Servicio General de Peluquería"));

                BigDecimal precioBase = new BigDecimal("25.50");
                totalEsperado = totalEsperado.add(precioBase);

                PdfPCell cellPrecio = new PdfPCell(new Phrase(precioBase.toString() + " €"));
                cellPrecio.setHorizontalAlignment(Element.ALIGN_RIGHT);

                table.addCell(cellCant);
                table.addCell(cellDesc);
                table.addCell(cellPrecio);
            }

            document.add(table);
            document.add(Chunk.NEWLINE);

            // 6. TOTAL a pagar / cobrado
            Paragraph totalP = new Paragraph("TOTAL FACTURA: " + totalEsperado + " €",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.RED));
            totalP.setAlignment(Element.ALIGN_RIGHT);
            document.add(totalP);

            // 7. Pie de Página Comercial
            document.add(Chunk.NEWLINE);
            Paragraph pie = new Paragraph("Gracias por confiar en Peluquería Marco G.\n¡Esperamos verte pronto!",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, BaseColor.GRAY));
            pie.setAlignment(Element.ALIGN_CENTER);
            document.add(pie);

            document.close();

        } catch (DocumentException e) {
            log.error("Ocurrió un error catastrófico montando el PDF de iText", e);
            throw new RuntimeException("Error en sistema de facturación PDF.");
        }

        return out.toByteArray();
    }

    private BigDecimal precioVenta(Servicio servicio) {
        if (servicio.getPrecioDescuento() != null && servicio.getPrecioDescuento().compareTo(BigDecimal.ZERO) > 0) {
            return servicio.getPrecioDescuento();
        }
        return servicio.getPrecio() != null ? servicio.getPrecio() : BigDecimal.ZERO;
    }
}
