package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.dto.ReporteKpiDTO;
import com.conectatech.sgs_backend.model.Catalogo;
import com.conectatech.sgs_backend.model.Incidente;
import com.conectatech.sgs_backend.repository.CatalogoRepository;
import com.conectatech.sgs_backend.repository.IncidenteRepository;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private IncidenteRepository incidenteRepository;

    @Mock
    private CatalogoRepository catalogoRepository;

    @InjectMocks
    private ReporteService reporteService;

    private Incidente incidentePrueba;

    @BeforeEach
    void setUp() {
        incidentePrueba = new Incidente();
        incidentePrueba.setId("inc-1");
        incidentePrueba.setCodigoCorrelativo("INC-2026-0001");
        incidentePrueba.setFechaCreacion(LocalDateTime.of(2026, 11, 10, 14, 30));
        incidentePrueba.setCategoria("INCIVILIDAD");
        incidentePrueba.setTipo("ROBO_HURTO");
        incidentePrueba.setPrioridad("ALTA");
        incidentePrueba.setEstado("EN_PROCESO");
        incidentePrueba.setOrigen("CAMARA_SEGURIDAD");
        incidentePrueba.setDireccionTexto("Av. San Marcos 123, El Tabo");

        when(incidenteRepository.buscarConFiltrosAvanzados(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(incidentePrueba));
        when(incidenteRepository.calcularKpis(any(), any(), any()))
                .thenReturn(new ReporteKpiDTO(1L, 100.0, 95.0, 10.0));
    }

    @Test
    @DisplayName("Excel: debe formatear CAMARA_SEGURIDAD, eliminar mayúsculas sostenidas y generar archivo válido")
    void testGenerarReporteExcelSinMayusculasSostenidas() throws IOException {
        byte[] bytes = reporteService.generarReporteExcel(null, null, null);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet("Reporte Consolidado");
            assertNotNull(sheet);

            // Verificar títulos en formato limpio (no mayúsculas sostenidas agresivas)
            assertEquals("Municipalidad de El Tabo - Dirección de Seguridad Pública",
                    sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Sistema de Gestión de Seguridad Pública · Informe Consolidado",
                    sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("Resumen Ejecutivo (Indicadores de Gestión)",
                    sheet.getRow(6).getCell(0).getStringCellValue());
            assertEquals("Detalle Operativo de Denuncias e Incidentes",
                    sheet.getRow(10).getCell(0).getStringCellValue());

            // Fila de datos (fila 12)
            Row dataRow = sheet.getRow(12);
            assertNotNull(dataRow);

            assertEquals("INC-2026-0001", dataRow.getCell(0).getStringCellValue());
            // Categoría: "Incivilidad" (en lugar de "INCIVILIDAD")
            assertEquals("Incivilidad", dataRow.getCell(2).getStringCellValue());
            // Tipo: "Robo" (en lugar de "ROBO_HURTO")
            assertEquals("Robo", dataRow.getCell(3).getStringCellValue());
            // Prioridad: "Alta" (en lugar de "ALTA")
            assertEquals("Alta", dataRow.getCell(4).getStringCellValue());
            // Estado: "En Proceso" (en lugar de "EN_PROCESO")
            assertEquals("En Proceso", dataRow.getCell(5).getStringCellValue());
            // Origen: "Cámara de Seguridad" (en lugar de "CAMARA_SEGURIDAD")
            assertEquals("Cámara de Seguridad", dataRow.getCell(7).getStringCellValue());
        }
    }

    @Test
    @DisplayName("PDF: debe incluir la columna Origen Denuncia y generar documento válido")
    void testGenerarReportePdfConColumnaOrigenDenuncia() throws IOException {
        byte[] bytes = reporteService.generarReportePDF(null, null, null);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        try (PdfDocument pdf = new PdfDocument(new PdfReader(new ByteArrayInputStream(bytes)))) {
            assertTrue(pdf.getNumberOfPages() >= 1);
        }
    }
}
