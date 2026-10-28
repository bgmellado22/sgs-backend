package com.conectatech.sgs_backend.repository;

import com.conectatech.sgs_backend.model.BitacoraProcedimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface BitacoraProcedimientoRepositoryCustom {

    Page<BitacoraProcedimiento> buscarConFiltrosPaginado(
            String textoBusqueda,
            String modulo,
            String tipoOperacion,
            String rolActor,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            Pageable pageable
    );
}

