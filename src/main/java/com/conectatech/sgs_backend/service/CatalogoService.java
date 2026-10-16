package com.conectatech.sgs_backend.service;

import lombok.RequiredArgsConstructor;
import com.conectatech.sgs_backend.model.Catalogo;
import com.conectatech.sgs_backend.repository.CatalogoRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogoService {
    private final CatalogoRepository repository;

    @Cacheable(value = "catalogos", key = "#tipo.toUpperCase()")
    public List<Catalogo> obtenerPorTipo(String tipo) {
        return repository.findByTipoAndActivoTrue(tipo.toUpperCase());
    }
}
