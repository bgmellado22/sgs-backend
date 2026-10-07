package com.conectatech.sgs_backend.repository;

import com.conectatech.sgs_backend.model.Sector;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectorRepository extends MongoRepository<Sector, String> {
    List<Sector> findByActivoTrue();
}
