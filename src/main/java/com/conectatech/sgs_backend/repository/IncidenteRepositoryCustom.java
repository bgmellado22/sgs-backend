package com.conectatech.sgs_backend.repository;

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
}