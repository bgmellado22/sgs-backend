package com.conectatech.sgs_backend.controller;

import com.conectatech.sgs_backend.model.Catalogo;
import com.conectatech.sgs_backend.model.ParametroSistema;
import com.conectatech.sgs_backend.service.ConfiguracionService;
import jakarta.validation.Valid;
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
     * Retorna todos los parámetros del sistema (Tiempos de Respuesta, etc.)
     */
    @GetMapping("/parametros")
    public ResponseEntity<List<ParametroSistema>> obtenerParametros() {
        return ResponseEntity.ok(configuracionService.obtenerParametros());
    }

    /**
     * PUT /api/v1/configuracion/parametros
     * Actualiza masivamente los parámetros del sistema.
     * Seguridad: ADMINISTRADOR y OPERADOR.
     */
    @PutMapping("/parametros")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<List<ParametroSistema>> actualizarParametros(
            @RequestBody Map<String, String> parametros) {
        return ResponseEntity.ok(configuracionService.actualizarParametros(parametros));
    }

    /**
     * DELETE /api/v1/configuracion/parametros/{id}
     * Elimina un parámetro del sistema.
     * Seguridad: ADMINISTRADOR y OPERADOR.
     */
    @DeleteMapping("/parametros/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<Void> eliminarParametro(@PathVariable String id) {
        configuracionService.eliminarParametro(id);
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────
    //  CATÁLOGOS (Origen, Prioridad, Categoría)
    // ──────────────────────────────────────────────

    /**
     * GET /api/v1/configuracion/catalogos/{tipo}
     * Retorna todos los catálogos (activos e inactivos) del tipo indicado.
     * Seguridad: ADMINISTRADOR y OPERADOR.
     */
    @GetMapping("/catalogos/{tipo}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<List<Catalogo>> obtenerCatalogosAdmin(@PathVariable String tipo) {
        return ResponseEntity.ok(configuracionService.obtenerCatalogosAdmin(tipo));
    }

    /**
     * POST /api/v1/configuracion/catalogos/{tipo}
     * Crea un nuevo valor de catálogo del tipo indicado.
     * Tipos permitidos: origen, prioridad, categoria
     * Seguridad: ADMINISTRADOR y OPERADOR.
     * Auditoría: Registro forense automático en bitácora.
     */
    @PostMapping("/catalogos/{tipo}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<Catalogo> crearCatalogo(
            @PathVariable String tipo,
            @Valid @RequestBody Catalogo catalogo) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(configuracionService.crearCatalogo(tipo, catalogo));
    }

    /**
     * PUT /api/v1/configuracion/catalogos/{id}
     * Edita un valor de catálogo existente.
     * Seguridad: ADMINISTRADOR y OPERADOR.
     */
    @PutMapping("/catalogos/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<Catalogo> editarCatalogo(
            @PathVariable String id,
            @Valid @RequestBody Catalogo catalogo) {
        return ResponseEntity.ok(configuracionService.editarCatalogo(id, catalogo));
    }

    /**
     * DELETE /api/v1/configuracion/catalogos/{id}
     * Elimina un valor de catálogo.
     * Seguridad: ADMINISTRADOR y OPERADOR.
     */
    @DeleteMapping("/catalogos/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<Void> eliminarCatalogo(@PathVariable String id) {
        configuracionService.eliminarCatalogo(id);
        return ResponseEntity.noContent().build();
    }
}
