package com.conectatech.sgs_backend.repository;

import com.conectatech.sgs_backend.model.Patrullaje;
import com.conectatech.sgs_backend.model.enums.EstadoPatrullaje;
import com.conectatech.sgs_backend.model.enums.ResultadoProcedimiento;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PatrullajeRepository extends MongoRepository<Patrullaje, String> {

    List<Patrullaje> findByActivoTrueAndEstadoInOrderByFechaCreacionDesc(List<EstadoPatrullaje> estados);

    List<Patrullaje> findByInspectorIdAndEstadoIn(String inspectorId, List<EstadoPatrullaje> estados);

    Optional<Patrullaje> findTopByOrderByFechaCreacionDesc();

    long countByFechaCreacionBetween(LocalDateTime inicio, LocalDateTime fin);

    List<Patrullaje> findByResultadoProcedimientoAndEvidenciaFotoIdNotNullAndEvidenciaDepuradaFalseAndFechaDepuracionEvidenciaBefore(
            ResultadoProcedimiento resultadoProcedimiento,
            LocalDateTime fechaLimite
    );
}

