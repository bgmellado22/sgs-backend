package com.conectatech.sgs_backend.repository;

import com.conectatech.sgs_backend.model.Incidente;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidenteRepository extends MongoRepository<Incidente, String> {
    List<Incidente> findByEstado(String estado);

    List<Incidente> findByCategoria(String categoria);
}
