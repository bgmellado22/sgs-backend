package com.conectatech.sgs_backend.controller;

import lombok.RequiredArgsConstructor;
import com.conectatech.sgs_backend.model.Catalogo;
import com.conectatech.sgs_backend.service.CatalogoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalogos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CatalogoController {
    private final CatalogoService service;

    @GetMapping("/{tipo}")
    public ResponseEntity<List<Catalogo>> obtenerCatalogo(@PathVariable String tipo) {
        List<Catalogo> list = service.obtenerPorTipo(tipo);
        return ResponseEntity.ok(service.obtenerPorTipo(tipo));
    }
}
