package com.conectatech.sgs_backend.controller;

import com.conectatech.sgs_backend.dto.IncidenteRequestDTO;
import com.conectatech.sgs_backend.dto.IncidenteResponseDTO;
import com.conectatech.sgs_backend.model.BitacoraProcedimiento;
import com.conectatech.sgs_backend.service.IncidenteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.util.List;

@RestController
@RequestMapping("/api/v1/incidentes")
@CrossOrigin(origins = { "http://localhost:5173", "https://sgs-el-tabo-frontend.vercel.app" })
@RequiredArgsConstructor
public class IncidenteController {

    private final IncidenteService incidenteService;

    @GetMapping
    public ResponseEntity<List<IncidenteResponseDTO>> obtenerTodos() {
        System.out.println("TEST GET CALLED");
        return ResponseEntity.ok(incidenteService.obtenerTodos());
    }

    @GetMapping("/{id}/bitacora")
    public ResponseEntity<List<BitacoraProcedimiento>> obtenerBitacora(@PathVariable String id) {
        return ResponseEntity.ok(incidenteService.obtenerHistorial(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<IncidenteResponseDTO> crearIncidente(
            @Valid @RequestPart("incidente") IncidenteRequestDTO incidenteDTO,
            @RequestPart(value = "evidencia", required = false) MultipartFile foto) {

        IncidenteResponseDTO nuevoIncidente = incidenteService.crearIncidente(incidenteDTO);

        if (foto != null && !foto.isEmpty()) {
            System.out.println("Foto recibida. Nombre: " + foto.getOriginalFilename());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoIncidente);
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
