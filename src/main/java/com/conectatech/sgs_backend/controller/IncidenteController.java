package com.conectatech.sgs_backend.controller;

import com.conectatech.sgs_backend.dto.IncidenteRequestDTO;
import com.conectatech.sgs_backend.dto.IncidenteResponseDTO;
import com.conectatech.sgs_backend.model.BitacoraProcedimiento;
import com.conectatech.sgs_backend.service.IncidenteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.conectatech.sgs_backend.dto.DerivacionRequestDTO;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.data.mongodb.gridfs.GridFsResource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

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

    // Paginación del Servidor con Filtros Avanzados
    @GetMapping("/paginado")
    public ResponseEntity<Page<IncidenteResponseDTO>> obtenerIncidentesPaginado(
            @RequestParam(required = false) String textoBusqueda,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String prioridad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @PageableDefault(size = 10, sort = "fecha_creacion", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<IncidenteResponseDTO> pagina = incidenteService.buscarIncidentesAvanzadoPaginado(
                textoBusqueda, categoria, tipo, estado, prioridad, fechaInicio, fechaFin, pageable);
        return ResponseEntity.ok(pagina);
    }

    // Creación
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<IncidenteResponseDTO> crearIncidente(
            @RequestPart("incidente") @Valid IncidenteRequestDTO incidenteDTO,
            @RequestPart(value = "evidencia", required = false) MultipartFile foto) {

        IncidenteResponseDTO creado = incidenteService.crearIncidente(incidenteDTO, foto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    // Gestión Operativa: Derivación a Carabineros o Municipalidad
    @PatchMapping("/{id}/derivar")
    public ResponseEntity<IncidenteResponseDTO> derivarIncidente(
            @PathVariable("id") String id,
            @Valid @RequestBody DerivacionRequestDTO dto) {

        IncidenteResponseDTO actualizado = incidenteService.derivarIncidente(id, dto);
        return ResponseEntity.ok(actualizado);
    }

    // Adjuntar evidencia posterior a una denuncia
    @PostMapping(value = "/{id}/evidencia", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<IncidenteResponseDTO> adjuntarEvidencia(
            @PathVariable("id") String id,
            @RequestParam("evidencia") MultipartFile archivo) {

        IncidenteResponseDTO actualizado = incidenteService.adjuntarEvidencia(id, archivo);
        return ResponseEntity.ok(actualizado);
    }

    // Descarga y visualización inline de evidencias (imágenes)
    @GetMapping("/evidencias/{gridFsId}")
    public ResponseEntity<Resource> descargarEvidencia(@PathVariable("gridFsId") String gridFsId) {
        try {
            GridFsResource resource = incidenteService.obtenerRecursoEvidencia(gridFsId);
            String contentType = resource.getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Actualización Rápida de Estado
    @PatchMapping("/{id}/estado")
    public ResponseEntity<IncidenteResponseDTO> actualizarEstado(
            @PathVariable("id") String id,
            @RequestBody Map<String, String> request) {

        String nuevoEstado = request.get("estado");
        String motivo = request.get("motivo");
        IncidenteResponseDTO actualizado = incidenteService.actualizarEstado(id, nuevoEstado, motivo);
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

    // Buscar
    @GetMapping("/buscar")
    public ResponseEntity<List<IncidenteResponseDTO>> buscarIncidentes(
            @RequestParam(required = false) String textoBusqueda,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String prioridad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        List<IncidenteResponseDTO> resultados = incidenteService.buscarIncidentesAvanzado(
                textoBusqueda, categoria, tipo, estado, prioridad, fechaInicio, fechaFin);
        return ResponseEntity.ok(resultados);
    }
}