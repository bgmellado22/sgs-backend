package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.dto.ReporteKpiDTO;
import com.conectatech.sgs_backend.model.Incidente;
import com.conectatech.sgs_backend.repository.IncidenteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteService {

    private final IncidenteRepository incidenteRepository;

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FORMATO_FECHA_CORTO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generarReporteExcel(String categoria, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        List<Incidente> incidentes = incidenteRepository.buscarConFiltrosAvanzados(
                null, categoria, null, null, null, fechaInicio, fechaFin);
        ReporteKpiDTO kpis = incidenteRepository.calcularKpis(categoria, fechaInicio, fechaFin);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Reporte Consolidado");

            // Estilos
            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            titleStyle.setFont(titleFont);

            CellStyle subtitleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font subFont = workbook.createFont();
            subFont.setBold(true);
            subFont.setFontHeightInPoints((short) 10);
            subFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            subtitleStyle.setFont(subFont);

            CellStyle sectionStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font secFont = workbook.createFont();
            secFont.setBold(true);
            secFont.setFontHeightInPoints((short) 11);
            sectionStyle.setFont(secFont);

            CellStyle metaLabelStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font metaLabelFont = workbook.createFont();
            metaLabelFont.setBold(true);
            metaLabelStyle.setFont(metaLabelFont);

            CellStyle kpiHeaderStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font kpiHeadFont = workbook.createFont();
            kpiHeadFont.setBold(true);
            kpiHeaderStyle.setFont(kpiHeadFont);
            kpiHeaderStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            kpiHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            kpiHeaderStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
            kpiHeaderStyle.setBorderBottom(BorderStyle.THIN);
            kpiHeaderStyle.setBorderTop(BorderStyle.THIN);
            kpiHeaderStyle.setBorderLeft(BorderStyle.THIN);
            kpiHeaderStyle.setBorderRight(BorderStyle.THIN);

            CellStyle kpiValueStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font kpiValFont = workbook.createFont();
            kpiValFont.setBold(true);
            kpiValFont.setFontHeightInPoints((short) 12);
            kpiValueStyle.setFont(kpiValFont);
            kpiValueStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
            kpiValueStyle.setBorderBottom(BorderStyle.THIN);
            kpiValueStyle.setBorderTop(BorderStyle.THIN);
            kpiValueStyle.setBorderLeft(BorderStyle.THIN);
            kpiValueStyle.setBorderRight(BorderStyle.THIN);

            CellStyle tableHeaderStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font thFont = workbook.createFont();
            thFont.setBold(true);
            tableHeaderStyle.setFont(thFont);
            tableHeaderStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            tableHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            tableHeaderStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
            tableHeaderStyle.setBorderBottom(BorderStyle.MEDIUM);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // 1. Encabezado institucional
            Row row0 = sheet.createRow(0);
            org.apache.poi.ss.usermodel.Cell c0 = row0.createCell(0);
            c0.setCellValue("MUNICIPALIDAD DE EL TABO - DIRECCIÓN DE SEGURIDAD PÚBLICA");
            c0.setCellStyle(titleStyle);

            Row row1 = sheet.createRow(1);
            org.apache.poi.ss.usermodel.Cell c1 = row1.createCell(0);
            c1.setCellValue("SISTEMA DE GESTIÓN DE SEGURIDAD PÚBLICA · INFORME CONSOLIDADO");
            c1.setCellStyle(subtitleStyle);

            // 2. Parámetros del Reporte
            String rangoFechas = (fechaInicio != null && fechaFin != null)
                    ? fechaInicio.format(FORMATO_FECHA_CORTO) + " al " + fechaFin.format(FORMATO_FECHA_CORTO)
                    : "Histórico Completo";
            String catTexto = (categoria != null && !categoria.isBlank()) ? categoria : "Todas las Categorías";

            Row row3 = sheet.createRow(3);
            org.apache.poi.ss.usermodel.Cell lblPer = row3.createCell(0);
            lblPer.setCellValue("Período Consultado:");
            lblPer.setCellStyle(metaLabelStyle);
            row3.createCell(1).setCellValue(rangoFechas);
            org.apache.poi.ss.usermodel.Cell lblCat = row3.createCell(4);
            lblCat.setCellValue("Categoría:");
            lblCat.setCellStyle(metaLabelStyle);
            row3.createCell(5).setCellValue(catTexto);

            Row row4 = sheet.createRow(4);
            org.apache.poi.ss.usermodel.Cell lblEmi = row4.createCell(0);
            lblEmi.setCellValue("Fecha de Emisión:");
            lblEmi.setCellStyle(metaLabelStyle);
            row4.createCell(1).setCellValue(LocalDateTime.now().format(FORMATO_FECHA_HORA));
            org.apache.poi.ss.usermodel.Cell lblTot = row4.createCell(4);
            lblTot.setCellValue("Total Incidentes:");
            lblTot.setCellStyle(metaLabelStyle);
            row4.createCell(5).setCellValue(incidentes.size());

            // 3. Resumen Ejecutivo (KPIs)
            Row row6 = sheet.createRow(6);
            org.apache.poi.ss.usermodel.Cell secKpi = row6.createCell(0);
            secKpi.setCellValue("RESUMEN EJECUTIVO (INDICADORES DE GESTIÓN)");
            secKpi.setCellStyle(sectionStyle);

            Row row7 = sheet.createRow(7);
            String[] kpiHeaders = { "Volumen Operativo", "Tasa de Resolución", "Cumplimiento SLA", "Criticidad Zonal" };
            for (int i = 0; i < kpiHeaders.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = row7.createCell(i);
                cell.setCellValue(kpiHeaders[i]);
                cell.setCellStyle(kpiHeaderStyle);
            }

            Row row8 = sheet.createRow(8);
            org.apache.poi.ss.usermodel.Cell k0 = row8.createCell(0);
            k0.setCellValue(kpis != null ? kpis.getVolumenOperativoTotal() : incidentes.size());
            k0.setCellStyle(kpiValueStyle);

            org.apache.poi.ss.usermodel.Cell k1 = row8.createCell(1);
            k1.setCellValue(
                    kpis != null ? String.format(Locale.US, "%.1f%%", kpis.getTasaResolucionEfectiva()) : "0.0%");
            k1.setCellStyle(kpiValueStyle);

            org.apache.poi.ss.usermodel.Cell k2 = row8.createCell(2);
            k2.setCellValue(kpis != null ? String.format(Locale.US, "%.1f%%", kpis.getCumplimientoSla()) : "0.0%");
            k2.setCellStyle(kpiValueStyle);

            org.apache.poi.ss.usermodel.Cell k3 = row8.createCell(3);
            k3.setCellValue(kpis != null ? String.format(Locale.US, "%.1f%%", kpis.getIndiceCriticidad()) : "0.0%");
            k3.setCellStyle(kpiValueStyle);

            // 4. Detalle de Incidentes
            Row row10 = sheet.createRow(10);
            org.apache.poi.ss.usermodel.Cell secDet = row10.createCell(0);
            secDet.setCellValue("DETALLE OPERATIVO DE DENUNCIAS E INCIDENTES");
            secDet.setCellStyle(sectionStyle);

            Row row11 = sheet.createRow(11);
            String[] columnas = {
                    "Código", "Fecha Registro", "Categoría", "Tipo", "Prioridad",
                    "Estado", "Dirección / Sector", "Origen Denuncia", "Fecha Cierre",
                    "Resolución (min)", "Estado SLA"
            };

            for (int i = 0; i < columnas.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = row11.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(tableHeaderStyle);
            }

            // Filas de datos
            int rowIdx = 12;
            for (Incidente inc : incidentes) {
                Row row = sheet.createRow(rowIdx++);

                crearCelda(row, 0, inc.getCodigoCorrelativo(), dataStyle);
                crearCelda(row, 1,
                        inc.getFechaCreacion() != null ? inc.getFechaCreacion().format(FORMATO_FECHA_HORA) : "-",
                        dataStyle);
                crearCelda(row, 2, inc.getCategoria() != null ? inc.getCategoria() : "-", dataStyle);
                crearCelda(row, 3, inc.getTipo() != null ? inc.getTipo() : "-", dataStyle);
                crearCelda(row, 4, inc.getPrioridad() != null ? inc.getPrioridad() : "-", dataStyle);
                crearCelda(row, 5, inc.getEstado() != null ? inc.getEstado() : "-", dataStyle);
                crearCelda(row, 6, inc.getDireccionTexto() != null ? inc.getDireccionTexto() : "No especificada",
                        dataStyle);
                crearCelda(row, 7, inc.getOrigen() != null ? inc.getOrigen() : "-", dataStyle);
                crearCelda(row, 8, inc.getFechaCierre() != null ? inc.getFechaCierre().format(FORMATO_FECHA_HORA) : "-",
                        dataStyle);
                crearCelda(row, 9,
                        inc.getTiempoResolucionMinutos() != null ? String.valueOf(inc.getTiempoResolucionMinutos())
                                : "-",
                        dataStyle);
                crearCelda(row, 10, evaluarEstadoSla(inc), dataStyle);
            }

            // Auto-ajuste de columnas
            for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
                // Asegurar ancho mínimo para legibilidad
                if (sheet.getColumnWidth(i) < 3200) {
                    sheet.setColumnWidth(i, 3500);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error al generar el reporte Excel: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar el reporte Excel: " + e.getMessage(), e);
        }
    }

    public byte[] generarReportePDF(String categoria, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        List<Incidente> incidentes = incidenteRepository.buscarConFiltrosAvanzados(
                null, categoria, null, null, null, fechaInicio, fechaFin);
        ReporteKpiDTO kpis = incidenteRepository.calcularKpis(categoria, fechaInicio, fechaFin);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            // Usar orientación A4 vertical con márgenes ejecutivos de 32pt
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(32, 32, 32, 32);

            // Paleta de colores ejecutiva
            DeviceRgb primaryColor = new DeviceRgb(26, 54, 93); // #1A365D Azul Marino Institucional
            DeviceRgb slateHeader = new DeviceRgb(241, 245, 249); // #F1F5F9 Gris Suave
            DeviceRgb kpiBoxBg = new DeviceRgb(248, 250, 252); // #F8FAFC Fondo tarjetas
            DeviceRgb borderColor = new DeviceRgb(226, 232, 240); // #E2E8F0 Bordes
            DeviceRgb textMuted = new DeviceRgb(100, 116, 139); // #64748B Texto secundario
            DeviceRgb textDark = new DeviceRgb(15, 23, 42); // #0F172A Texto principal

            // 1. Membrete Institucional con Logo
            Table headerTable = new Table(UnitValue.createPercentArray(new float[] { 14, 86 }));
            headerTable.setWidth(UnitValue.createPercentValue(100));
            headerTable.setBorder(Border.NO_BORDER);
            headerTable.setMarginBottom(10);

            // Celda Logo
            Cell logoCell = new Cell().setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE);
            try {
                ClassPathResource logoRes = new ClassPathResource("logo-el-tabo.png");
                if (logoRes.exists()) {
                    ImageData imgData = ImageDataFactory.create(logoRes.getContentAsByteArray());
                    Image logo = new Image(imgData);
                    logo.setMaxHeight(52);
                    logo.setAutoScale(true);
                    logoCell.add(logo);
                }
            } catch (Exception e) {
                log.warn("No se pudo cargar el logo municipal en el PDF: {}", e.getMessage());
            }
            headerTable.addCell(logoCell);

            // Celda Títulos
            Cell titleCell = new Cell().setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE);
            titleCell.add(new Paragraph("Municipalidad de El Tabo")
                    .setBold().setFontSize(13).setFontColor(primaryColor).setMargin(0));
            titleCell.add(new Paragraph("Dirección de Seguridad Pública")
                    .setBold().setFontSize(10).setFontColor(new DeviceRgb(51, 65, 85)).setMargin(0));
            titleCell.add(new Paragraph("Sistema de Gestión de Seguridad Pública - Informe Ejecutivo de Operaciones")
                    .setFontSize(8.5f).setItalic().setFontColor(textMuted).setMargin(0));
            headerTable.addCell(titleCell);

            document.add(headerTable);

            // Línea divisoria de acento
            SolidLine line = new SolidLine(1.5f);
            line.setColor(primaryColor);
            LineSeparator separator = new LineSeparator(line);
            separator.setMarginBottom(12);
            document.add(separator);

            // 2. Tarjeta de Metadatos y Filtros del Reporte
            Table metaTable = new Table(UnitValue.createPercentArray(new float[] { 50, 50 }));
            metaTable.setWidth(UnitValue.createPercentValue(100));
            metaTable.setBackgroundColor(slateHeader);
            metaTable.setBorder(new SolidBorder(borderColor, 1));
            metaTable.setPadding(6);
            metaTable.setMarginBottom(14);

            String rangoFechas = (fechaInicio != null && fechaFin != null)
                    ? fechaInicio.format(FORMATO_FECHA_CORTO) + " al " + fechaFin.format(FORMATO_FECHA_CORTO)
                    : "Histórico General";
            String catTexto = (categoria != null && !categoria.isBlank()) ? categoria : "Todas las Categorías";

            Cell metaCol1 = new Cell().setBorder(Border.NO_BORDER);
            metaCol1.add(new Paragraph().add(new Paragraph("Período Evaluado: ").setBold().setFontSize(8.5f))
                    .add(new Paragraph(rangoFechas).setFontSize(8.5f)).setMargin(1));
            metaCol1.add(new Paragraph().add(new Paragraph("Categoría de Filtro: ").setBold().setFontSize(8.5f))
                    .add(new Paragraph(catTexto).setFontSize(8.5f)).setMargin(1));
            metaTable.addCell(metaCol1);

            Cell metaCol2 = new Cell().setBorder(Border.NO_BORDER);
            metaCol2.add(new Paragraph().add(new Paragraph("Fecha de Emisión: ").setBold().setFontSize(8.5f))
                    .add(new Paragraph(LocalDateTime.now().format(FORMATO_FECHA_HORA)).setFontSize(8.5f)).setMargin(1));
            metaCol2.add(new Paragraph().add(new Paragraph("Total de Casos: ").setBold().setFontSize(8.5f))
                    .add(new Paragraph(incidentes.size() + " registros consolidados").setFontSize(8.5f)).setMargin(1));
            metaTable.addCell(metaCol2);

            document.add(metaTable);

            // 3. Resumen Ejecutivo (KPIs Destacados)
            Paragraph kpiTitle = new Paragraph("1. RESUMEN EJECUTIVO")
                    .setBold().setFontSize(10.5f).setFontColor(primaryColor).setMarginBottom(6);
            document.add(kpiTitle);

            Table kpiTable = new Table(UnitValue.createPercentArray(new float[] { 25, 25, 25, 25 }));
            kpiTable.setWidth(UnitValue.createPercentValue(100));
            kpiTable.setMarginBottom(16);

            long volTotal = kpis != null ? kpis.getVolumenOperativoTotal() : incidentes.size();
            double tasaRes = kpis != null ? kpis.getTasaResolucionEfectiva() : 0.0;
            double cumpSla = kpis != null ? kpis.getCumplimientoSla() : 0.0;
            double critZon = kpis != null ? kpis.getIndiceCriticidad() : 0.0;

            agregarTarjetaKpi(kpiTable, "VOLUMEN OPERATIVO", String.valueOf(volTotal), "Denuncias totales", kpiBoxBg,
                    borderColor, primaryColor);
            agregarTarjetaKpi(kpiTable, "TASA RESOLUCIÓN", String.format(Locale.US, "%.1f%%", tasaRes),
                    "Meta operativa: ≥ 75%", kpiBoxBg, borderColor, primaryColor);
            agregarTarjetaKpi(kpiTable, "CUMPLIMIENTO SLA", String.format(Locale.US, "%.1f%%", cumpSla),
                    "Meta operativa: ≥ 80%", kpiBoxBg, borderColor, primaryColor);
            agregarTarjetaKpi(kpiTable, "CRITICIDAD ZONAL", String.format(Locale.US, "%.1f%%", critZon),
                    "Meta operativa: ≤ 35%", kpiBoxBg, borderColor, primaryColor);

            document.add(kpiTable);

            // 4. Detalle Operativo de Incidentes
            Paragraph tableTitle = new Paragraph("2. DETALLE DE INCIDENTES Y REGISTROS")
                    .setBold().setFontSize(10.5f).setFontColor(primaryColor).setMarginBottom(6);
            document.add(tableTitle);

            // Anchos de columna proporcionales ajustados para A4 vertical
            float[] anchos = { 1.3f, 1.4f, 2.1f, 1.0f, 1.1f, 2.7f, 1.1f };
            Table table = new Table(anchos);
            table.setWidth(UnitValue.createPercentValue(100));

            String[] cabeceras = { "Código", "Fecha", "Tipo / Denuncia", "Prioridad", "Estado", "Ubicación / Dirección",
                    "SLA" };
            for (String cabecera : cabeceras) {
                Cell cell = new Cell()
                        .add(new Paragraph(cabecera).setBold().setFontSize(8).setFontColor(ColorConstants.BLACK))
                        .setTextAlignment(TextAlignment.CENTER)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE)
                        .setBackgroundColor(slateHeader)
                        .setBorder(new SolidBorder(borderColor, 1))
                        .setPadding(4);
                table.addHeaderCell(cell);
            }

            // Filas de incidentes
            boolean zebra = false;
            for (Incidente inc : incidentes) {
                com.itextpdf.kernel.colors.Color bgFila = zebra ? new DeviceRgb(248, 250, 252) : ColorConstants.WHITE;
                zebra = !zebra;

                table.addCell(
                        crearCeldaPdf(inc.getCodigoCorrelativo(), 7.5f, TextAlignment.CENTER, bgFila, borderColor));
                table.addCell(crearCeldaPdf(
                        inc.getFechaCreacion() != null ? inc.getFechaCreacion().format(FORMATO_FECHA_HORA) : "-", 7.5f,
                        TextAlignment.CENTER, bgFila, borderColor));

                String tipoCompleto = (inc.getTipo() != null ? inc.getTipo() : "") +
                        (inc.getCategoria() != null ? "\n(" + inc.getCategoria() + ")" : "");
                table.addCell(crearCeldaPdf(tipoCompleto, 7.5f, TextAlignment.LEFT, bgFila, borderColor));

                table.addCell(crearCeldaPdf(inc.getPrioridad() != null ? inc.getPrioridad() : "-", 7.5f,
                        TextAlignment.CENTER, bgFila, borderColor));
                table.addCell(crearCeldaPdf(inc.getEstado() != null ? inc.getEstado() : "-", 7.5f, TextAlignment.CENTER,
                        bgFila, borderColor));
                table.addCell(
                        crearCeldaPdf(inc.getDireccionTexto() != null ? inc.getDireccionTexto() : "No especificada",
                                7.5f, TextAlignment.LEFT, bgFila, borderColor));
                table.addCell(crearCeldaPdf(evaluarEstadoSla(inc), 7.5f, TextAlignment.CENTER, bgFila, borderColor));
            }

            document.add(table);

            // 5. Pie de página formal
            Paragraph footer = new Paragraph(
                    "\nSistema de Gestión de Seguridad Pública. Municipalidad De El Tabo -  Información sujeta a reserva legal y operativa")
                    .setFontSize(7.5f)
                    .setItalic()
                    .setFontColor(textMuted)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(12);
            document.add(footer);

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Error crítico al generar el reporte PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error crítico al generar el reporte PDF: " + e.getMessage(), e);
        }
    }

    public ReporteKpiDTO obtenerKpisPorFecha(String categoria, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return incidenteRepository.calcularKpis(categoria, fechaInicio, fechaFin);
    }

    private void agregarTarjetaKpi(Table table, String titulo, String valor, String subtitulo,
            DeviceRgb bg, DeviceRgb border, DeviceRgb textPrimary) {
        Cell cell = new Cell()
                .setBackgroundColor(bg)
                .setBorder(new SolidBorder(border, 1))
                .setPadding(6)
                .setTextAlignment(TextAlignment.CENTER);

        cell.add(new Paragraph(titulo).setBold().setFontSize(7.5f).setFontColor(new DeviceRgb(100, 116, 139))
                .setMargin(0));
        cell.add(new Paragraph(valor).setBold().setFontSize(14f).setFontColor(textPrimary).setMargin(2));
        cell.add(new Paragraph(subtitulo).setFontSize(6.5f).setFontColor(new DeviceRgb(100, 116, 139)).setMargin(0));

        table.addCell(cell);
    }

    private Cell crearCeldaPdf(String texto, float fontSize, TextAlignment align, com.itextpdf.kernel.colors.Color bg,
            DeviceRgb border) {
        return new Cell()
                .add(new Paragraph(texto != null ? texto : "").setFontSize(fontSize))
                .setTextAlignment(align)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBackgroundColor(bg)
                .setBorder(new SolidBorder(border, 0.5f))
                .setPadding(4);
    }

    private void crearCelda(Row row, int colIdx, String valor, CellStyle style) {
        org.apache.poi.ss.usermodel.Cell cell = row.createCell(colIdx);
        cell.setCellValue(valor != null ? valor : "");
        cell.setCellStyle(style);
    }

    private String evaluarEstadoSla(Incidente inc) {
        if ("Resuelto".equalsIgnoreCase(inc.getEstado()) || "Cerrado".equalsIgnoreCase(inc.getEstado())) {
            if (inc.getTiempoResolucionMinutos() != null) {
                long meta = inc.getSlaMinutosObjetivo() != null ? inc.getSlaMinutosObjetivo() : 2880L;
                return inc.getTiempoResolucionMinutos() <= meta ? "Cumplido" : "Fuera plazo";
            }
            return "Resuelto";
        }
        if (inc.getFechaVencimientoSla() != null && LocalDateTime.now().isAfter(inc.getFechaVencimientoSla())) {
            return "Vencido";
        }
        return "En curso";
    }
}
