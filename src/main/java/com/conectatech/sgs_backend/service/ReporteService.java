package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.dto.ReporteKpiDTO;
import com.conectatech.sgs_backend.model.Incidente;
import com.conectatech.sgs_backend.repository.IncidenteRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final IncidenteRepository incidenteRepository;

    public byte[] generarReporteExcel(String categoria, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        List<Incidente> incidentes = incidenteRepository.buscarConFiltrosAvanzados(
                null, categoria, null, null, null, fechaInicio, fechaFin);

        // Crear libro de excel en memoria
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Consolidado de Incidentes");

            // Estilos para la cabecera
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Crear fila de cabecera
            Row headerRow = sheet.createRow(0);
            String[] columnas = { "Código", "Categoría", "Tipo", "Estado", "Prioridad", "Fecha de Registro" };
            for (int i = 0; i < columnas.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            // Poblar los datos
            int rowIdx = 1;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (Incidente inc : incidentes) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(inc.getCodigoCorrelativo());
                row.createCell(1).setCellValue(inc.getCategoria());
                row.createCell(2).setCellValue(inc.getTipo());
                row.createCell(3).setCellValue(inc.getEstado());
                row.createCell(4).setCellValue(inc.getPrioridad());
                row.createCell(5).setCellValue(
                        inc.getFechaCreacion() != null ? inc.getFechaCreacion().format(formatter) : "N/A");
            }

            // Auto-ajustar el ancho de las columnas
            for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Escribir a binario y retornar
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el reporte Excel", e);
        }
    }

    public byte[] generarReportePDF(String categoria, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        List<Incidente> incidentes = incidenteRepository.buscarConFiltrosAvanzados(
                null, categoria, null, null, null, fechaInicio, fechaFin);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Inicializar el escritor y el documento PDF
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Crear el título del documento
            Paragraph titulo = new Paragraph("Consolidado de Incidentes - SGS El Tabo")
                    .setBold()
                    .setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(titulo);

            // Crear la tabla
            float[] columnWidths = { 1, 2, 2, 1, 1, 2 };
            Table table = new Table(columnWidths);
            table.setWidth(UnitValue.createPercentValue(100));

            // Encabezados de la tabla
            String[] cabeceras = { "Código", "Categoría", "Tipo", "Estado", "Prioridad", "Fecha" };
            for (String cabecera : cabeceras) {
                Cell cell = new Cell()
                        .add(new Paragraph(cabecera).setBold().setFontSize(10))
                        .setTextAlignment(TextAlignment.CENTER)
                        .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY);
                table.addHeaderCell(cell);
            }

            // Poblar las filas con los datos de MongoDB
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            for (Incidente inc : incidentes) {
                table.addCell(new Cell().add(new Paragraph(inc.getCodigoCorrelativo()).setFontSize(9)));
                table.addCell(new Cell()
                        .add(new Paragraph(inc.getCategoria() != null ? inc.getCategoria() : "").setFontSize(9)));
                table.addCell(new Cell().add(new Paragraph(inc.getTipo() != null ? inc.getTipo() : "").setFontSize(9)));
                table.addCell(
                        new Cell().add(new Paragraph(inc.getEstado() != null ? inc.getEstado() : "").setFontSize(9)));
                table.addCell(new Cell()
                        .add(new Paragraph(inc.getPrioridad() != null ? inc.getPrioridad() : "").setFontSize(9)));
                table.addCell(new Cell().add(
                        new Paragraph(inc.getFechaCreacion() != null ? inc.getFechaCreacion().format(formatter) : "N/A")
                                .setFontSize(9)));
            }

            // Renderizar tabla en el lienzo y cerrar
            document.add(table);
            document.close();

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error crítico al generar el reporte PDF: " + e.getMessage());
        }
    }

    public ReporteKpiDTO obtenerKpisPorFecha(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return incidenteRepository.calcularKpis(fechaInicio, fechaFin);
    }
}
