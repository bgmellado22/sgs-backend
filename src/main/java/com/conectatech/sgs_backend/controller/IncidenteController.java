package com.conectatech.sgs_backend.controller;

import com.conectatech.sgs_backend.model.Incidente;
import com.conectatech.sgs_backend.service.IncidenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidentes")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class IncidenteController {

    private final IncidenteService incidenteService;

    @GetMapping
    public ResponseEntity<List<Incidente>> obtenerTodos() {
        return ResponseEntity.ok(incidenteService.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<Incidente> crearIncidente(@RequestBody Incidente nuevoIncidente) {
        Incidente incidenteGuardado = incidenteService.crearIncidente(nuevoIncidente);
        return new ResponseEntity<>(incidenteGuardado, HttpStatus.CREATED);
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<Incidente>> obtenerPorEstado(@PathVariable String estado) {
        return ResponseEntity.ok(incidenteService.obtenerPorEstado(estado));
    }
}
