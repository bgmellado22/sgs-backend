package com.conectatech.sgs_backend.repository;

import com.conectatech.sgs_backend.model.BitacoraProcedimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BitacoraProcedimientoRepository extends MongoRepository<BitacoraProcedimiento, String>, BitacoraProcedimientoRepositoryCustom {

    // Ver detalle de un incidente en específico
    List<BitacoraProcedimiento> findByIncidenteIdOrderByFechaModificacionDesc(String incidenteId);

    // Vista global de la bitácora sin paginar (legacy/compatibilidad)
    List<BitacoraProcedimiento> findAllByOrderByFechaModificacionDesc();

    // Vista global de la bitácora paginada
    Page<BitacoraProcedimiento> findAllByOrderByFechaModificacionDesc(Pageable pageable);
}
