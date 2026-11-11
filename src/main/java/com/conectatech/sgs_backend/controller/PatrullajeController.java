package com.conectatech.sgs_backend.controller;

import com.conectatech.sgs_backend.dto.CierrePatrullajeDTO;
import com.conectatech.sgs_backend.dto.CrearPatrullajeDTO;
import com.conectatech.sgs_backend.dto.PatrullajeDTO;
import com.conectatech.sgs_backend.dto.TelemetriaInspectorDTO;
import com.conectatech.sgs_backend.service.PatrullajeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patrullajes")
@RequiredArgsConstructor
@CrossOrigin(origins = { "http://localhost:5173", "https://sgs-el-tabo-frontend.vercel.app" })
public class PatrullajeController {

    private final PatrullajeService patrullajeService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<PatrullajeDTO> crearPatrullaje(@Valid @RequestBody CrearPatrullajeDTO dto) {
        PatrullajeDTO creado = patrullajeService.crearPatrullaje(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/activos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR', 'INSPECTOR')")
    public ResponseEntity<List<PatrullajeDTO>> obtenerPatrullajesActivos() {
        return ResponseEntity.ok(patrullajeService.listarActivos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR', 'INSPECTOR')")
    public ResponseEntity<PatrullajeDTO> obtenerPatrullajePorId(@PathVariable String id) {
        return ResponseEntity.ok(patrullajeService.obtenerPorId(id));
    }

    @PatchMapping("/{id}/en-sitio")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR', 'INSPECTOR')")
    public ResponseEntity<PatrullajeDTO> marcarEnSitio(@PathVariable String id) {
        return ResponseEntity.ok(patrullajeService.marcarEnSitio(id));
    }

    @PostMapping(value = "/{id}/cerrar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR', 'INSPECTOR')")
    public ResponseEntity<PatrullajeDTO> cerrarPatrullaje(
            @PathVariable String id,
            @RequestPart("datos") @Valid CierrePatrullajeDTO datos,
            @RequestPart(value = "evidencia", required = false) MultipartFile evidencia) {
        return ResponseEntity.ok(patrullajeService.cerrarPatrullaje(id, datos, evidencia));
    }

    @GetMapping("/{id}/evidencia")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR', 'INSPECTOR')")
    public ResponseEntity<Resource> descargarEvidencia(@PathVariable String id) {
        Resource resource = patrullajeService.descargarEvidencia(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }

    @PostMapping("/telemetria")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR', 'INSPECTOR')")
    public ResponseEntity<Void> recibirTelemetriaHttp(@Valid @RequestBody TelemetriaInspectorDTO telemetria) {
        // Retransmite por WebSocket STOMP al tópico de telemetría de inspectores
        messagingTemplate.convertAndSend("/topic/telemetria-inspectores", telemetria);
        return ResponseEntity.accepted().build();
    }
}

