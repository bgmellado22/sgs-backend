package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.dto.IncidenteRequestDTO;
import com.conectatech.sgs_backend.dto.IncidenteResponseDTO;
import com.conectatech.sgs_backend.model.Incidente;
import com.conectatech.sgs_backend.model.Notificacion;
import com.conectatech.sgs_backend.model.BitacoraProcedimiento;
import com.conectatech.sgs_backend.model.Usuario;
import com.conectatech.sgs_backend.model.enums.TipoNotificacion;
import com.conectatech.sgs_backend.service.NotificacionWebSocketService;
import com.conectatech.sgs_backend.repository.IncidenteRepository;
import com.conectatech.sgs_backend.repository.BitacoraProcedimientoRepository;
import com.conectatech.sgs_backend.repository.ParametroSistemaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IncidenteService {
        private final IncidenteRepository incidenteRepository;
        private final NotificacionWebSocketService websocketService;
        // Repositorio de bitácora
        private final BitacoraProcedimientoRepository bitacoraRepository;
        private final ParametroSistemaRepository parametroRepository;

        // Guardar
        public IncidenteResponseDTO crearIncidente(IncidenteRequestDTO dto) {
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
