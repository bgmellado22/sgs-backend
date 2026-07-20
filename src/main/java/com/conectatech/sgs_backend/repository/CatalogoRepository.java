package com.conectatech.sgs_backend.repository;

import com.conectatech.sgs_backend.model.Catalogo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatalogoRepository extends MongoRepository<Catalogo, String> {
    List<Catalogo> findByTipoAndActivoTrue(String tipo);
}
