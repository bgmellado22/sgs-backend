package com.conectatech.sgs_backend.controller;

import com.conectatech.sgs_backend.model.Catalogo;
import com.conectatech.sgs_backend.model.ParametroSistema;
import com.conectatech.sgs_backend.service.ConfiguracionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/configuracion")
@CrossOrigin(origins = { "http://localhost:5173", "https://sgs-el-tabo-frontend.vercel.app" })
@RequiredArgsConstructor
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;

    // ──────────────────────────────────────────────
    //  PARÁMETROS DEL SISTEMA
    // ──────────────────────────────────────────────

    /**
     * GET /api/v1/configuracion/parametros
     * Retorna todos los parámetros del sistema (SLAs, etc.)
     */
    @GetMapping("/parametros")
    public ResponseEntity<List<ParametroSistema>> obtenerParametros() {
        return ResponseEntity.ok(configuracionService.obtenerParametros());
    }

    /**
     * PUT /api/v1/configuracion/parametros
     * Actualiza masivamente los parámetros del sistema.
     * Seguridad: Solo ADMINISTRADOR.
     */
    @PutMapping("/parametros")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<ParametroSistema>> actualizarParametros(
            @RequestBody Map<String, String> parametros) {
        return ResponseEntity.ok(configuracionService.actualizarParametros(parametros));
    }

    // ──────────────────────────────────────────────
    //  CATÁLOGOS (Origen, Prioridad, Categoría)
    // ──────────────────────────────────────────────

    /**
     * POST /api/v1/configuracion/catalogos/{tipo}
     * Crea un nuevo valor de catálogo del tipo indicado.
     * Tipos permitidos: origen, prioridad, categoria
     * Seguridad: Solo ADMINISTRADOR.
     * Auditoría: Registro forense automático en bitácora.
     */
    @PostMapping("/catalogos/{tipo}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Catalogo> crearCatalogo(
            @PathVariable String tipo,
            @RequestBody Catalogo catalogo) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(configuracionService.crearCatalogo(tipo, catalogo));
    }
}
