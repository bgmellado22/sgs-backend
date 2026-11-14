package com.conectatech.sgs_backend.controller;

import com.conectatech.sgs_backend.model.Sector;
import com.conectatech.sgs_backend.service.SectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sectores")
@CrossOrigin(origins = { "http://localhost:5173", "https://sgs-el-tabo-frontend.vercel.app" })
@RequiredArgsConstructor
public class SectorController {

    private final SectorService sectorService;

    /**
     * Retorna todos los sectores comunales activos para el mapa táctico y filtros.
     */
    @GetMapping
    public ResponseEntity<List<Sector>> listarActivos() {
        return ResponseEntity.ok(sectorService.obtenerActivos());
    }

    /**
     * Retorna todos los sectores (activos e inactivos) para el gestor de catálogos.
     */
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<List<Sector>> listarTodos() {
        return ResponseEntity.ok(sectorService.obtenerTodos());
    }

    /**
     * Crea un nuevo sector comunal con su zona o centro en el mini mapa.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Sector> crearSector(@RequestBody Map<String, Object> payload) {
        String nombre = (String) payload.get("nombre");
        if (nombre == null || nombre.isBlank()) {
            nombre = (String) payload.get("etiqueta");
        }

        Double latitud = null;
        if (payload.get("latitud") != null) {
            latitud = Double.valueOf(payload.get("latitud").toString());
        }
        Double longitud = null;
        if (payload.get("longitud") != null) {
            longitud = Double.valueOf(payload.get("longitud").toString());
        }

        if (nombre == null || nombre.isBlank() || latitud == null || longitud == null) {
            return ResponseEntity.badRequest().build();
        }

        Sector sector = new Sector();
        sector.setNombre(nombre.trim());
        sector.setCentro(new GeoJsonPoint(longitud, latitud));
        sector.setActivo(true);

        return ResponseEntity.status(HttpStatus.CREATED).body(sectorService.crearSector(sector));
    }

    /**
     * Actualiza un sector comunal existente.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Sector> actualizarSector(@PathVariable String id, @RequestBody Map<String, Object> payload) {
        String nombre = (String) payload.get("nombre");
        if (nombre == null || nombre.isBlank()) {
            nombre = (String) payload.get("etiqueta");
        }

        Double latitud = null;
        if (payload.get("latitud") != null) {
            latitud = Double.valueOf(payload.get("latitud").toString());
        }
        Double longitud = null;
        if (payload.get("longitud") != null) {
            longitud = Double.valueOf(payload.get("longitud").toString());
        }

        Boolean activo = payload.get("activo") != null ? Boolean.valueOf(payload.get("activo").toString()) : true;

        Sector datos = new Sector();
        if (nombre != null) {
            datos.setNombre(nombre.trim());
        }
        if (latitud != null && longitud != null) {
            datos.setCentro(new GeoJsonPoint(longitud, latitud));
        }
        datos.setActivo(activo);

        return ResponseEntity.ok(sectorService.actualizarSector(id, datos));
    }
}

