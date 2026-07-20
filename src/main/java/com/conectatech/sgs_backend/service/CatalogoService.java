package com.conectatech.sgs_backend.service;

import lombok.RequiredArgsConstructor;
import com.conectatech.sgs_backend.model.Catalogo;
import com.conectatech.sgs_backend.repository.CatalogoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogoService {
    private final CatalogoRepository repository;

    public List<Catalogo> obtenerPorTipo(String tipo) {
        return repository.findByTipoAndActivoTrue(tipo.toUpperCase());
    }
}
