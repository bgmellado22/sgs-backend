package com.conectatech.sgs_backend.controller;

import com.conectatech.sgs_backend.dto.IncidenteRequestDTO;
import com.conectatech.sgs_backend.dto.IncidenteResponseDTO;
import com.conectatech.sgs_backend.dto.IncidenteEstadoRequestDTO;
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
import com.conectatech.sgs_backend.dto.DerivacionPolicialDTO;
import com.conectatech.sgs_backend.dto.DespachoMunicipalDTO;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.data.mongodb.gridfs.GridFsResource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;

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

    // Obtener un incidente por ID
    @GetMapping("/{id}")
    public ResponseEntity<IncidenteResponseDTO> obtenerPorId(@PathVariable("id") String id) {
        return ResponseEntity.ok(incidenteService.obtenerPorId(id));
    }

    // Paginación del Servidor con Filtros Avanzados
    @GetMapping("/paginado")
    public ResponseEntity<Page<IncidenteResponseDTO>> obtenerIncidentesPaginado(
            @RequestParam(required = false) String textoBusqueda,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String prioridad,
            @RequestParam(required = false) Boolean soloInactivos,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @PageableDefault(size = 10, sort = "fecha_creacion", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<IncidenteResponseDTO> pagina = incidenteService.buscarIncidentesAvanzadoPaginado(
                textoBusqueda, categoria, tipo, estado, prioridad, soloInactivos, fechaInicio, fechaFin, pageable);
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

    // Gestión Operativa: Derivación Interinstitucional a Carabineros de Chile (Multipart)
    @PostMapping(value = "/{id}/derivar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<IncidenteResponseDTO> derivarACarabineros(
            @PathVariable("id") String id,
            @RequestPart("datos") @Valid DerivacionPolicialDTO dto,
            @RequestPart(value = "comprobantePdf", required = false) MultipartFile comprobantePdf) {

        if (comprobantePdf != null && !comprobantePdf.isEmpty()) {
            if (comprobantePdf.getSize() > 10 * 1024 * 1024) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo PDF no debe exceder 10 MB.");
            }
            String contentType = comprobantePdf.getContentType();
            if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se admiten documentos en formato application/pdf.");
            }
        }

        IncidenteResponseDTO actualizado = incidenteService.derivarACarabineros(id, dto, comprobantePdf);
        return ResponseEntity.ok(actualizado);
    }

    // Gestión Operativa: Despacho Interno Municipal (Seguridad Ciudadana / OS-14)
    @PostMapping("/{id}/despachar")
    public ResponseEntity<IncidenteResponseDTO> despacharMunicipal(
            @PathVariable("id") String id,
            @Valid @RequestBody DespachoMunicipalDTO dto) {

        IncidenteResponseDTO actualizado = incidenteService.despacharMunicipal(id, dto);
        return ResponseEntity.ok(actualizado);
    }

    // Descarga y visualización inline de comprobante PDF de derivación policial
    @GetMapping("/{id}/comprobante")
    public ResponseEntity<Resource> obtenerComprobantePdf(@PathVariable("id") String id) {
        GridFsResource recurso = incidenteService.obtenerComprobanteDerivacion(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"comprobante-derivacion-" + id + ".pdf\"")
                .body(recurso);
    }

    // Compatibilidad legada: PATCH derivar
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

    // Actualización Rápida de Estado con Bloqueo Optimista
    @PatchMapping("/{id}/estado")
    public ResponseEntity<IncidenteResponseDTO> actualizarEstado(
            @PathVariable("id") String id,
            @Valid @RequestBody IncidenteEstadoRequestDTO request) {

        IncidenteResponseDTO actualizado = incidenteService.actualizarEstado(id, request);
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

    // Borrado Lógico con justificación forense en bitácora
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarIncidente(
            @PathVariable("id") String id,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(value = "motivo", required = false) String motivoParam) {

        String motivo = motivoParam;
        if ((motivo == null || motivo.isBlank()) && body != null && body.containsKey("motivo")) {
            Object val = body.get("motivo");
            if (val != null) {
                motivo = val.toString();
            }
        }

        if (motivo != null && !motivo.isBlank()) {
            incidenteService.eliminarIncidente(id, motivo);
        } else {
            incidenteService.eliminarIncidente(id);
        }
        return ResponseEntity.noContent().build();
    }

    // Reactivación de denuncia inactiva (Acceso EXCLUSIVO para Administradores)
    @PatchMapping("/{id}/reactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<IncidenteResponseDTO> reactivarIncidente(
            @PathVariable("id") String id,
            @RequestBody(required = false) Map<String, String> request) {

        String motivo = (request != null && request.containsKey("motivo"))
                ? request.get("motivo")
                : null;

        IncidenteResponseDTO reactivado = incidenteService.reactivarIncidente(id, motivo);
        return ResponseEntity.ok(reactivado);
    }

    // Historial de UN solo incidente
    @GetMapping("/{id}/bitacora")
    public ResponseEntity<List<BitacoraProcedimiento>> obtenerBitacora(
            @PathVariable("id") String id) {
        return ResponseEntity.ok(incidenteService.obtenerHistorial(id));
    }

    // Historial Global Paginado con Filtros (Acceso EXCLUSIVO para Administradores)
    @GetMapping("/bitacora/global")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Page<BitacoraProcedimiento>> obtenerBitacoraGlobal(
            @RequestParam(required = false) String textoBusqueda,
            @RequestParam(required = false) String modulo,
            @RequestParam(required = false) String tipoOperacion,
            @RequestParam(required = false) String rolActor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @PageableDefault(size = 20, sort = "fechaModificacion", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<BitacoraProcedimiento> pagina = incidenteService.obtenerHistorialGlobalPaginado(
                textoBusqueda, modulo, tipoOperacion, rolActor, fechaInicio, fechaFin, pageable);
        return ResponseEntity.ok(pagina);
    }

    // Buscar
    @GetMapping("/buscar")
    public ResponseEntity<List<IncidenteResponseDTO>> buscarIncidentes(
            @RequestParam(required = false) String textoBusqueda,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String prioridad,
            @RequestParam(required = false) Boolean soloInactivos,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        List<IncidenteResponseDTO> resultados = incidenteService.buscarIncidentesAvanzado(
                textoBusqueda, categoria, tipo, estado, prioridad, soloInactivos, fechaInicio, fechaFin);
        return ResponseEntity.ok(resultados);
    }
}
