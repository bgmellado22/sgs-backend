package com.conectatech.sgs_backend.repository;

import com.conectatech.sgs_backend.model.ParametroSistema;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParametroSistemaRepository extends MongoRepository<ParametroSistema, String> {

    Optional<ParametroSistema> findByClave(String clave);
}
