package com.conectatech.sgs_backend.controller;

import com.conectatech.sgs_backend.dto.DashboardEstadisticasDTO;
import com.conectatech.sgs_backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@CrossOrigin(origins = { "http://localhost:5173", "https://sgs-el-tabo-frontend.vercel.app" })
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * GET /api/v1/dashboard/estadisticas
     * Volumen y variación semanal, tasa de resolución, tendencia de 7 días,
     * distribución horaria, hotspots territoriales y distribución por categoría.
     */
    @GetMapping("/estadisticas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR', 'INSPECTOR')")
    public ResponseEntity<DashboardEstadisticasDTO> obtenerEstadisticas() {
        return ResponseEntity.ok(dashboardService.obtenerEstadisticas());
    }
}
