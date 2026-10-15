package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.dto.DerivacionRequestDTO;
import com.conectatech.sgs_backend.dto.IncidenteRequestDTO;
import com.conectatech.sgs_backend.dto.IncidenteResponseDTO;
import com.conectatech.sgs_backend.model.Incidente;
import com.conectatech.sgs_backend.model.Evidencia;
import com.conectatech.sgs_backend.model.Notificacion;
import com.conectatech.sgs_backend.model.BitacoraProcedimiento;
import com.conectatech.sgs_backend.model.Usuario;
import com.conectatech.sgs_backend.model.enums.TipoNotificacion;
import com.conectatech.sgs_backend.service.NotificacionWebSocketService;
import com.conectatech.sgs_backend.repository.IncidenteRepository;
import com.conectatech.sgs_backend.repository.BitacoraProcedimientoRepository;
import com.conectatech.sgs_backend.repository.ParametroSistemaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidenteService {
        private final IncidenteRepository incidenteRepository;
        private final SectorService sectorService;
        private final NotificacionWebSocketService websocketService;
        private final BitacoraProcedimientoRepository bitacoraRepository;
        private final ParametroSistemaRepository parametroRepository;
        private final GridFsTemplate gridFsTemplate;

        // Guardar sin archivo (compatibilidad)
        public IncidenteResponseDTO crearIncidente(IncidenteRequestDTO dto) {
                return crearIncidente(dto, null);
        }

        // Guardar con soporte de evidencia (foto / video)
        public IncidenteResponseDTO crearIncidente(IncidenteRequestDTO dto, MultipartFile foto) {
                Incidente incidente = new Incidente();

                incidente.setCategoria(dto.getCategoria());
                incidente.setTipo(dto.getTipo());
                incidente.setDescripcion(dto.getDescripcion());
                incidente.setPrioridad(dto.getPrioridad() != null ? dto.getPrioridad() : "MEDIA");
                incidente.setOrigen(dto.getOrigen());
                GeoJsonPoint puntoGPS = new GeoJsonPoint(dto.getLongitud(), dto.getLatitud());
                incidente.setLocation(puntoGPS);
                incidente.setDireccionTexto(dto.getDireccionTexto());

                incidente.setCodigoCorrelativo("INC-" + (int) (Math.random() * 900 + 100));
                incidente.setEstado("Pendiente");
                incidente.setFechaCreacion(LocalDateTime.now());

                asignarVencimientoSla(incidente);

                // Procesar evidencia fotográfica/video si fue adjuntada
                if (foto != null && !foto.isEmpty()) {
                        try {
                                ObjectId fileId = gridFsTemplate.store(
                                        foto.getInputStream(),
                                        foto.getOriginalFilename(),
                                        foto.getContentType()
                                );
                                Evidencia evidencia = Evidencia.builder()
                                        .id(fileId.toHexString())
                                        .nombre(foto.getOriginalFilename())
                                        .tipo(foto.getContentType())
                                        .tamano(foto.getSize())
                                        .url("/api/v1/incidentes/evidencias/" + fileId.toHexString())
                                        .fechaCarga(LocalDateTime.now())
                                        .build();
                                if (incidente.getEvidencias() == null) {
                                        incidente.setEvidencias(new ArrayList<>());
                                }
                                incidente.getEvidencias().add(evidencia);
                        } catch (IOException e) {
                                log.error("Error al almacenar la evidencia en GridFS", e);
                        }
                }

                Incidente guardado = incidenteRepository.save(incidente);

                // Obtener actor autenticado
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                Usuario actor = (Usuario) auth.getPrincipal();

                // Registro en bitácora con diferenciación por rol
                String etiquetaRol = switch (actor.getRol()) {
                        case CIUDADANO -> "Ciudadano";
                        case OPERADOR -> "Operador Central";
                        case INSPECTOR -> "Inspector en Terreno";
                        case ADMINISTRADOR -> "Administrador";
                };

                String mensajeNotificacion;
                String comentarioBitacora;

                if (actor.getRol() == com.conectatech.sgs_backend.model.enums.RolUsuario.CIUDADANO) {
                        mensajeNotificacion = etiquetaRol + " " + actor.getNombreCompleto()
                                        + " ha ingresado una denuncia: " + guardado.getCodigoCorrelativo();
                        comentarioBitacora = "Denuncia ingresada por ciudadano " + actor.getNombreCompleto();
                } else {
                        mensajeNotificacion = etiquetaRol + " " + actor.getNombreCompleto()
                                        + " ha registrado una nueva denuncia: " + guardado.getCodigoCorrelativo();
                        comentarioBitacora = "Denuncia registrada por " + etiquetaRol.toLowerCase() + " "
                                        + actor.getNombreCompleto();
                }

                registrarAuditoria(guardado.getId(), guardado.getCodigoCorrelativo(), actor, "Creación",
                                null, "Pendiente", comentarioBitacora);

                // Notificación global por WebSocket
                Notificacion alertaCreacion = Notificacion.builder()
                                .titulo("Nueva Denuncia")
                                .mensaje(mensajeNotificacion)
                                .tipo(TipoNotificacion.INFORMATIVO)
                                .referenciaId(guardado.getId())
                                .usuarioDestinoId("GLOBAL")
                                .build();

                websocketService.despacharAlertaGlobal(alertaCreacion);

                return mapToDTO(guardado);
        }

        // Adjuntar evidencia posterior a una denuncia existente
        public IncidenteResponseDTO adjuntarEvidencia(String id, MultipartFile archivo) {
                Incidente incidente = incidenteRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Incidente no encontrado: " + id));

                if (archivo == null || archivo.isEmpty()) {
                        throw new IllegalArgumentException("El archivo de evidencia está vacío.");
                }

                try {
                        ObjectId fileId = gridFsTemplate.store(
                                archivo.getInputStream(),
                                archivo.getOriginalFilename(),
                                archivo.getContentType()
                        );
                        Evidencia evidencia = Evidencia.builder()
                                .id(fileId.toHexString())
                                .nombre(archivo.getOriginalFilename())
                                .tipo(archivo.getContentType())
                                .tamano(archivo.getSize())
                                .url("/api/v1/incidentes/evidencias/" + fileId.toHexString())
                                .fechaCarga(LocalDateTime.now())
                                .build();

                        if (incidente.getEvidencias() == null) {
                                incidente.setEvidencias(new ArrayList<>());
                        }
                        incidente.getEvidencias().add(evidencia);

                        Incidente guardado = incidenteRepository.save(incidente);

                        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                        Usuario actor = (Usuario) auth.getPrincipal();
                        registrarAuditoria(guardado.getId(), guardado.getCodigoCorrelativo(), actor,
                                        "Evidencia", "Adjunto previo", archivo.getOriginalFilename(),
                                        "Carga de evidencia multimedia (" + archivo.getContentType() + ")");

                        return mapToDTO(guardado);
                } catch (IOException e) {
                        log.error("Error al adjuntar evidencia a incidente: {}", id, e);
                        throw new RuntimeException("Error al procesar el archivo: " + e.getMessage());
                }
        }

        // Obtener recurso GridFS para streaming de fotos/videos
        public GridFsResource obtenerRecursoEvidencia(String gridFsId) {
                GridFSFile file = null;
                if (ObjectId.isValid(gridFsId)) {
                        file = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(new ObjectId(gridFsId))));
                }
                if (file == null) {
                        file = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(gridFsId)));
                }
                if (file == null) {
                        throw new RuntimeException("Evidencia no encontrada con ID: " + gridFsId);
                }
                return gridFsTemplate.getResource(file);
        }

        // Gestión Operativa: Derivar denuncia a Carabineros o Municipalidad
        public IncidenteResponseDTO derivarIncidente(String id, DerivacionRequestDTO dto) {
                Incidente incidente = incidenteRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Incidente no encontrado con el ID: " + id));

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                Usuario actor = (Usuario) auth.getPrincipal();

                String entidadAnterior = incidente.getDerivacionEntidad() != null
                                ? incidente.getDerivacionEntidad()
                                : "Sin derivar";

                incidente.setDerivado(true);
                incidente.setDerivacionEntidad(dto.getEntidad());
                incidente.setDerivacionDetalle(dto.getDetalle());
                incidente.setDerivacionInspectorId(dto.getInspectorId());
                incidente.setDerivacionInspectorNombre(dto.getInspectorNombre());
                incidente.setDerivacionObservacion(dto.getObservacion());
                incidente.setFechaDerivacion(LocalDateTime.now());
                incidente.setDerivadoPor(actor.getNombreCompleto());

                // Si la denuncia estaba "Pendiente", la derivación inicia la gestión ("En proceso")
                String estadoAnterior = incidente.getEstado();
                if ("Pendiente".equalsIgnoreCase(estadoAnterior)) {
                        incidente.setEstado("En proceso");
                        registrarAuditoria(incidente.getId(), incidente.getCodigoCorrelativo(), actor,
                                        "Estado", "Pendiente", "En proceso",
                                        "Transición automática a En proceso por derivación operativa");
                }

                Incidente guardado = incidenteRepository.save(incidente);

                String valorNuevoAuditoria = dto.getEntidad().equalsIgnoreCase("CARABINEROS")
                                ? "Carabineros de Chile" + (dto.getDetalle() != null && !dto.getDetalle().isBlank() ? " (" + dto.getDetalle() + ")" : "")
                                : "Seguridad Municipal" + (dto.getInspectorNombre() != null && !dto.getInspectorNombre().isBlank() ? " (" + dto.getInspectorNombre() + ")" : "");

                registrarAuditoria(guardado.getId(), guardado.getCodigoCorrelativo(), actor,
                                "Derivación Operativa", entidadAnterior, valorNuevoAuditoria,
                                dto.getObservacion() != null && !dto.getObservacion().isBlank() ? dto.getObservacion() : "Derivación ejecutada desde panel de gestión");

                // Notificación global por WebSocket
                String destinatario = dto.getEntidad().equalsIgnoreCase("CARABINEROS") ? "Carabineros de Chile" : "Seguridad Municipal";
                Notificacion alertaDerivacion = Notificacion.builder()
                                .titulo("Incidente Derivado")
                                .mensaje("El operador " + actor.getNombreCompleto() + " derivó el incidente "
                                                + guardado.getCodigoCorrelativo() + " a " + destinatario)
                                .tipo(TipoNotificacion.INFORMATIVO)
                                .referenciaId(guardado.getId())
                                .usuarioDestinoId("GLOBAL")
                                .build();
                websocketService.despacharAlertaGlobal(alertaDerivacion);

                return mapToDTO(guardado);
        }

        // Listar
        public List<IncidenteResponseDTO> obtenerTodos() {
                return incidenteRepository.findByActivoTrue()
                                .stream()
                                .map(this::mapToDTO)
                                .collect(Collectors.toList());
        }

        // Traducir Entidad a DTO
        private IncidenteResponseDTO mapToDTO(Incidente incidente) {
                IncidenteResponseDTO dto = new IncidenteResponseDTO();
                dto.setId(incidente.getId());
                dto.setCodigoCorrelativo(incidente.getCodigoCorrelativo());
                dto.setCategoria(incidente.getCategoria());
                dto.setTipo(incidente.getTipo());
                dto.setDescripcion(incidente.getDescripcion());
                dto.setPrioridad(incidente.getPrioridad());
                dto.setEstado(incidente.getEstado());
                dto.setFechaCreacion(incidente.getFechaCreacion());
                dto.setFechaCierre(incidente.getFechaCierre());
                dto.setTiempoResolucionMinutos(incidente.getTiempoResolucionMinutos());
                dto.setSlaMinutosObjetivo(incidente.getSlaMinutosObjetivo());
                dto.setFechaVencimientoSla(incidente.getFechaVencimientoSla());
                dto.setOrigen(incidente.getOrigen());
                // Extraer coordenadas del geojsonpoint de mongodb
                if (incidente.getLocation() != null) {
                        dto.setLongitud(incidente.getLocation().getX());
                        dto.setLatitud(incidente.getLocation().getY());
                }
                dto.setDireccionTexto(incidente.getDireccionTexto());
                dto.setSector(sectorService.resolverSector(dto.getLongitud(), dto.getLatitud()));

                // Gestión Operativa (Derivación)
                dto.setDerivado(incidente.isDerivado());
                dto.setDerivacionEntidad(incidente.getDerivacionEntidad());
                dto.setDerivacionDetalle(incidente.getDerivacionDetalle());
                dto.setDerivacionInspectorId(incidente.getDerivacionInspectorId());
                dto.setDerivacionInspectorNombre(incidente.getDerivacionInspectorNombre());
                dto.setDerivacionObservacion(incidente.getDerivacionObservacion());
                dto.setFechaDerivacion(incidente.getFechaDerivacion());
                dto.setDerivadoPor(incidente.getDerivadoPor());

                // Evidencias Multimedia
                dto.setEvidencias(incidente.getEvidencias() != null ? incidente.getEvidencias() : Collections.emptyList());

                return dto;
        }

        // Método para auditar de forma dinámica
        private void registrarAuditoria(String incidenteId, String codigoCorrelativo, Usuario actor, String campo,
                        String valorAnt, String valorNue,
                        String comentario) {
                // Evitar generar registros basura si los valores son idénticos
                if (Objects.equals(valorAnt, valorNue))
                        return;

                BitacoraProcedimiento registro = BitacoraProcedimiento.builder()
                                .incidenteId(incidenteId)
                                .codigoCorrelativo(codigoCorrelativo)
                                .usuarioId(actor.getId())
                                .nombreActor(actor.getNombreCompleto())
                                .rolActor(actor.getRol().name())
                                .campoModificado(campo)
                                .valorAnterior(valorAnt)
                                .valorNuevo(valorNue)
                                .comentario(comentario)
                                .fechaModificacion(LocalDateTime.now())
                                .build();

                bitacoraRepository.save(registro);
        }

        // Método para actualizar el estado de un incidente con auditoría
        public IncidenteResponseDTO actualizarEstado(String id, String nuevoEstado) {
                Incidente incidenteExistente = incidenteRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Error: Incidente no encontrado con el ID: " + id));

                String estadoAnterior = incidenteExistente.getEstado();

                if (estadoAnterior != null && estadoAnterior.equals(nuevoEstado)) {
                        return mapToDTO(incidenteExistente);
                }

                incidenteExistente.setEstado(nuevoEstado);

                if ("Resuelto".equals(nuevoEstado) || "Cerrado".equals(nuevoEstado)) {
                        if (incidenteExistente.getFechaCierre() == null) {
                                incidenteExistente.setFechaCierre(LocalDateTime.now());
                                long minutos = ChronoUnit.MINUTES.between(incidenteExistente.getFechaCreacion(), incidenteExistente.getFechaCierre());
                                incidenteExistente.setTiempoResolucionMinutos(minutos);
                        }
                } else {
                        incidenteExistente.setFechaCierre(null);
                        incidenteExistente.setTiempoResolucionMinutos(null);
                }

                Incidente actualizado = incidenteRepository.save(incidenteExistente);

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                Usuario actor = (Usuario) auth.getPrincipal();

                // Llamar al método de auditoría
                registrarAuditoria(actualizado.getId(), actualizado.getCodigoCorrelativo(), actor, "Estado",
                                estadoAnterior, nuevoEstado,
                                "Actualización de estado desde panel operativo");

                // Gatillo global de websocket para notificar a todos los usuarios
                Notificacion alertaGlobal = Notificacion.builder()
                                .titulo("Cambio de Estado")
                                .mensaje("El operador " + actor.getNombreCompleto() + " cambió el incidente "
                                                + actualizado.getCodigoCorrelativo() + " a: " + nuevoEstado)
                                .tipo(TipoNotificacion.INFORMATIVO)
                                .referenciaId(actualizado.getId())
                                .usuarioDestinoId("GLOBAL")
                                .build();

                websocketService.despacharAlertaGlobal(alertaGlobal);

                return mapToDTO(actualizado);
        }

        // Método para edición completa de un incidente con auditoría
        public IncidenteResponseDTO editarIncidente(String id, IncidenteRequestDTO dto) {
                Incidente incidente = incidenteRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Error: Incidente no encontrado con el ID: " + id));

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                Usuario actor = (Usuario) auth.getPrincipal();

                // Auditar y Actualizar Prioridad
                if (dto.getPrioridad() != null && !dto.getPrioridad().equals(incidente.getPrioridad())) {
                        registrarAuditoria(incidente.getId(), incidente.getCodigoCorrelativo(), actor, "Prioridad",
                                        incidente.getPrioridad(), dto.getPrioridad(),
                                        "Edición de prioridad por parte de " + actor.getNombreCompleto());
                        incidente.setPrioridad(dto.getPrioridad());
                        
                        // Recalcular SLA si cambia la prioridad
                        asignarVencimientoSla(incidente);
                }

                // Auditar y Actualizar Origen
                if (dto.getOrigen() != null && !dto.getOrigen().equals(incidente.getOrigen())) {
                        registrarAuditoria(incidente.getId(), incidente.getCodigoCorrelativo(), actor, "Origen",
                                        incidente.getOrigen(), dto.getOrigen(),
                                        "Edición de origen por parte de " + actor.getNombreCompleto());
                        incidente.setOrigen(dto.getOrigen());
                }

                // Auditar y Actualizar Categoría
                if (dto.getCategoria() != null && !dto.getCategoria().equals(incidente.getCategoria())) {
                        registrarAuditoria(incidente.getId(), incidente.getCodigoCorrelativo(), actor, "Categoría",
                                        incidente.getCategoria(), dto.getCategoria(),
                                        "Edición de categoría por parte de " + actor.getNombreCompleto());
                        incidente.setCategoria(dto.getCategoria());
                }

                // Auditar y Actualizar Tipo
                if (dto.getTipo() != null && !dto.getTipo().equals(incidente.getTipo())) {
                        registrarAuditoria(incidente.getId(), incidente.getCodigoCorrelativo(), actor, "Tipo",
                                        incidente.getTipo(), dto.getTipo(),
                                        "Edición de tipo por parte de " + actor.getNombreCompleto());
                        incidente.setTipo(dto.getTipo());
                }

                // Auditar y Actualizar Estado si fue modificado en la edición
                if (dto.getEstado() != null && !dto.getEstado().equals(incidente.getEstado())) {
                        String estadoAnterior = incidente.getEstado();
                        String nuevoEstado = dto.getEstado();
                        incidente.setEstado(nuevoEstado);

                        if ("Resuelto".equals(nuevoEstado) || "Cerrado".equals(nuevoEstado)) {
                                if (incidente.getFechaCierre() == null) {
                                        incidente.setFechaCierre(LocalDateTime.now());
                                        long minutos = ChronoUnit.MINUTES.between(incidente.getFechaCreacion(), incidente.getFechaCierre());
                                        incidente.setTiempoResolucionMinutos(minutos);
                                }
                        } else {
                                incidente.setFechaCierre(null);
                                incidente.setTiempoResolucionMinutos(null);
                        }

                        registrarAuditoria(incidente.getId(), incidente.getCodigoCorrelativo(), actor, "Estado",
                                        estadoAnterior, nuevoEstado,
                                        "Edición de estado administrativo por " + actor.getNombreCompleto());
                }

                Incidente actualizado = incidenteRepository.save(incidente);

                // Despacho de alerta global
                Notificacion alertaEdicion = Notificacion.builder()
                                .titulo("Incidente Modificado")
                                .mensaje("El operador " + actor.getNombreCompleto()
                                                + " ha modificado los parámetros del incidente "
                                                + actualizado.getCodigoCorrelativo())
                                .tipo(TipoNotificacion.ALERTA)
                                .referenciaId(actualizado.getId())
                                .usuarioDestinoId("GLOBAL")
                                .build();
                websocketService.despacharAlertaGlobal(alertaEdicion);

                return mapToDTO(actualizado);
        }

        // Obtener historial de cambios de un incidente
        public List<BitacoraProcedimiento> obtenerHistorial(String incidenteId) {
                return bitacoraRepository.findByIncidenteIdOrderByFechaModificacionDesc(incidenteId);
        }

        // Obtener todo el historial
        public List<BitacoraProcedimiento> obtenerHistorialGlobal() {
                return bitacoraRepository.findAllByOrderByFechaModificacionDesc();
        }

        // Método para eliminar un incidente
        public void eliminarIncidente(String id) {
                Incidente incidenteExistente = incidenteRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Error: No se puede eliminar. Incidente no encontrado con ID: " + id));

                incidenteExistente.setActivo(false);

                incidenteRepository.save(incidenteExistente);

                // Registro en bitácora
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                Usuario actor = (Usuario) auth.getPrincipal();

                registrarAuditoria(incidenteExistente.getId(), incidenteExistente.getCodigoCorrelativo(), actor,
                                "Eliminación",
                                "Activo", "Eliminado",
                                "Denuncia eliminada por "
                                                + actor.getNombreCompleto());

                // Despacho de alerta global
                Notificacion alertaBorrado = Notificacion.builder()
                                .titulo("Incidente Eliminado")
                                .mensaje("El registro " + incidenteExistente.getCodigoCorrelativo()
                                                + " fue dado de baja por "
                                                + actor.getNombreCompleto())
                                .tipo(TipoNotificacion.ALERTA)
                                .referenciaId(incidenteExistente.getId())
                                .usuarioDestinoId("GLOBAL")
                                .build();
                websocketService.despacharAlertaGlobal(alertaBorrado);
        }

        // Método para buscar incidentes con filtros avanzados
        public List<IncidenteResponseDTO> buscarIncidentesAvanzado(
                        String textoBusqueda,
                        String categoria,
                        String tipo,
                        String estado,
                        String prioridad,
                        LocalDateTime fechaInicio,
                        LocalDateTime fechaFin) {

                List<Incidente> resultados = incidenteRepository.buscarConFiltrosAvanzados(
                                textoBusqueda,
                                categoria,
                                tipo,
                                estado,
                                prioridad,
                                fechaInicio,
                                fechaFin);

                return resultados.stream()
                                .map(this::mapToDTO)
                                .collect(java.util.stream.Collectors.toList());
        }

        private void asignarVencimientoSla(Incidente incidente) {
                if (incidente.getPrioridad() == null) return;
                
                String claveSla = "SLA_" + incidente.getPrioridad().toUpperCase() + "_MINUTOS";
                parametroRepository.findByClave(claveSla).ifPresent(param -> {
                        try {
                                long minutos = Long.parseLong(param.getValor());
                                incidente.setSlaMinutosObjetivo(minutos);
                                
                                LocalDateTime baseTime = incidente.getFechaCreacion() != null 
                                        ? incidente.getFechaCreacion() 
                                        : LocalDateTime.now();
                                        
                                incidente.setFechaVencimientoSla(baseTime.plusMinutes(minutos));
                        } catch (NumberFormatException e) {
                                // Ignorar si el formato es incorrecto
                        }
                });
        }
}
