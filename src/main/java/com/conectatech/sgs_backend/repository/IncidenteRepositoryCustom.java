package com.conectatech.sgs_backend.repository;

import com.conectatech.sgs_backend.dto.ReporteKpiDTO;
import com.conectatech.sgs_backend.model.Incidente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

public interface IncidenteRepositoryCustom {

    List<Incidente> buscarConFiltrosAvanzados(
            String textoBusqueda,
            String categoria,
            String tipo,
            String estado,
            String prioridad,
            boolean soloInactivos,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin);

    default List<Incidente> buscarConFiltrosAvanzados(
            String textoBusqueda,
            String categoria,
            String tipo,
            String estado,
            String prioridad,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin) {
        return buscarConFiltrosAvanzados(textoBusqueda, categoria, tipo, estado, prioridad, false, fechaInicio, fechaFin);
    }

    Page<Incidente> buscarConFiltrosAvanzadosPaginado(
            String textoBusqueda,
            String categoria,
            String tipo,
            String estado,
            String prioridad,
            boolean soloInactivos,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            Pageable pageable);

    default Page<Incidente> buscarConFiltrosAvanzadosPaginado(
            String textoBusqueda,
            String categoria,
            String tipo,
            String estado,
            String prioridad,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            Pageable pageable) {
        return buscarConFiltrosAvanzadosPaginado(textoBusqueda, categoria, tipo, estado, prioridad, false, fechaInicio, fechaFin, pageable);
    }

    // Método para KPI
    ReporteKpiDTO calcularKpis(String categoria, LocalDateTime fechaInicio, LocalDateTime fechaFin);
}