package com.conectatech.sgs_backend.controller;

import com.conectatech.sgs_backend.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/reportes")
@CrossOrigin(origins = { "http://localhost:5173", "http://sgs-el-tabo-frontend.vercel.app" })
@RequiredArgsConstructor
public class ReporteController {
    private final ReporteService reporteService;

    @GetMapping("/excel")
    public ResponseEntity<byte[]> descargarExcel(
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        byte[] archivoExcel = reporteService.generarReporteExcel(categoria, fechaInicio, fechaFin);

        HttpHeaders headers = new HttpHeaders();
        // Tipo MIME oficial para archivos .xlsx
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        // Nombre del archivo que recibirá el usuario
        headers.setContentDispositionFormData("attachment", "Consolidado_SGS.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(archivoExcel);
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> descargarPDF(
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        byte[] archivoPdf = reporteService.generarReportePDF(categoria, fechaInicio, fechaFin);

        HttpHeaders headers = new HttpHeaders();
        // Tipo MIME oficial para PDF
        headers.setContentType(MediaType.APPLICATION_PDF);
        // "attachment" fuerza la descarga
        headers.setContentDispositionFormData("attachment", "Consolidado_SGS.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(archivoPdf);
    }
}
