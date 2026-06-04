package com.conectatech.sgs_backend.controller;

import com.conectatech.sgs_backend.model.Incidente;
import com.conectatech.sgs_backend.repository.IncidenteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/incidentes")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class IncidenteController {

    private final IncidenteRepository incidenteRepository;

    @GetMapping
    public ResponseEntity<List<Incidente>> obtenerTodos() {
        List<Incidente> incidentes = incidenteRepository.findAll();
        return ResponseEntity.ok(incidentes);
    }

    @PostMapping
    public ResponseEntity<Incidente> crearIncidente(@RequestBody Incidente nuevoIncidente) {
        nuevoIncidente.setFechaCreacion(LocalDateTime.now());
        Incidente incidenteGuardado = incidenteRepository.save(nuevoIncidente);
        return new ResponseEntity<>(incidenteGuardado, HttpStatus.CREATED);
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<Incidente>> obtenerPorEstado(@PathVariable String estado) {
        List<Incidente> incidentes = incidenteRepository.findByEstado(estado);
        return ResponseEntity.ok(incidentes);
    }
}
