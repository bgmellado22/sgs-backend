package com.conectatech.sgs_backend.controller;

import com.conectatech.sgs_backend.dto.IncidenteRequestDTO;
import com.conectatech.sgs_backend.dto.IncidenteResponseDTO;
import com.conectatech.sgs_backend.model.BitacoraProcedimiento;
import com.conectatech.sgs_backend.service.IncidenteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/incidentes")
@CrossOrigin(origins = { "http://localhost:5173", "https://sgs-el-tabo-frontend.vercel.app" })
@RequiredArgsConstructor
public class IncidenteController {

    private final IncidenteService incidenteService;

    // Lectura
    @GetMapping
    public ResponseEntity<List<IncidenteResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(incidenteService.obtenerTodos());
    }

    // Creación
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<IncidenteResponseDTO> crearIncidente(
            @RequestPart("incidente") @Valid IncidenteRequestDTO incidenteDTO,
            @RequestPart(value = "evidencia", required = false) MultipartFile foto) {

        IncidenteResponseDTO creado = incidenteService.crearIncidente(incidenteDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    // Actualización Rápida de Estado
    @PatchMapping("/{id}/estado")
    public ResponseEntity<IncidenteResponseDTO> actualizarEstado(
            @PathVariable("id") String id,
            @RequestBody Map<String, String> request) {

        String nuevoEstado = request.get("estado");
        IncidenteResponseDTO actualizado = incidenteService.actualizarEstado(id, nuevoEstado);
        return ResponseEntity.ok(actualizado);
    }

    // Edición
    @PutMapping("/{id}")
    public ResponseEntity<IncidenteResponseDTO> editarIncidente(
            @PathVariable("id") String id,
            @Valid @RequestBody IncidenteRequestDTO dto) {

        IncidenteResponseDTO editado = incidenteService.editarIncidente(id, dto);
        return ResponseEntity.ok(editado);
    }

    // Borrado Lógico
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarIncidente(@PathVariable("id") String id) {
        incidenteService.eliminarIncidente(id);
        return ResponseEntity.noContent().build();
    }

    // Historial de UN solo incidente
    @GetMapping("/{id}/bitacora")
    public ResponseEntity<List<BitacoraProcedimiento>> obtenerBitacora(
            @PathVariable("id") String id) {
        return ResponseEntity.ok(incidenteService.obtenerHistorial(id));
    }

    // Historial Global
    @GetMapping("/bitacora/global")
    public ResponseEntity<List<BitacoraProcedimiento>> obtenerBitacoraGlobal() {
        return ResponseEntity.ok(incidenteService.obtenerHistorialGlobal());
    }
}