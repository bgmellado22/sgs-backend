package com.conectatech.sgs_backend.repository;

import com.conectatech.sgs_backend.model.BitacoraProcedimiento;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface BitacoraProcedimientoRepository extends MongoRepository<BitacoraProcedimiento, String> {

    // Ver detalle de un incidente en específico
    List<BitacoraProcedimiento> findByIncidenteIdOrderByFechaModificacionDesc(String incidenteId);

    // Vista global de la bitácora
    List<BitacoraProcedimiento> findAllByOrderByFechaModificacionDesc();
}
