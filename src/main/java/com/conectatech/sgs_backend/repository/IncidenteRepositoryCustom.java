package com.conectatech.sgs_backend.repository;

import com.conectatech.sgs_backend.dto.ReporteKpiDTO;
import com.conectatech.sgs_backend.model.Incidente;
import java.time.LocalDateTime;
import java.util.List;

public interface IncidenteRepositoryCustom {

    List<Incidente> buscarConFiltrosAvanzados(
            String textoBusqueda,
            String categoria,
            String tipo,
            String estado,
            String prioridad,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin);

    // Método para KPI
    ReporteKpiDTO calcularKpis(String categoria, LocalDateTime fechaInicio, LocalDateTime fechaFin);
}