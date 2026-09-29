package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.model.Incidente;
import com.conectatech.sgs_backend.repository.IncidenteRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

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
                Cell cell = headerRow.createCell(i);
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
}
