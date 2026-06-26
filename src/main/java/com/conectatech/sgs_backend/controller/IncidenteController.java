package com.conectatech.sgs_backend.controller;

import com.conectatech.sgs_backend.dto.IncidenteRequestDTO;
import com.conectatech.sgs_backend.dto.IncidenteResponseDTO;
import com.conectatech.sgs_backend.service.IncidenteService;
import jakarta.validation.Valid;
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
    public ResponseEntity<List<IncidenteResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(incidenteService.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<IncidenteResponseDTO> crearIncidente(@Valid @RequestBody IncidenteRequestDTO dto) {
        IncidenteResponseDTO response = incidenteService.crearIncidente(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<IncidenteResponseDTO> actualizarEstado(
            @PathVariable("id") String id,
            @RequestBody java.util.Map<String, String> body) {
        String nuevoEstado = body.get("estado");

        IncidenteResponseDTO response = incidenteService.actualizarEstado(id, nuevoEstado);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarIncidente(@PathVariable("id") String id) {
        incidenteService.eliminarIncidente(id);
        return ResponseEntity.noContent().build();
    }
}
