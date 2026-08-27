package com.conectatech.sgs_backend.repository;

import com.conectatech.sgs_backend.model.BitacoraProcedimiento;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface BitacoraProcedimientoRepository extends MongoRepository<BitacoraProcedimiento, String> {

    // Busca todo el historial de un incidente y lo ordena por fecha descendente
    List<BitacoraProcedimiento> findByIncidenteIdOrderByFechaModificacionDesc(String incidenteId);
}
