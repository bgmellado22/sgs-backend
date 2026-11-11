package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.model.BitacoraProcedimiento;
import com.conectatech.sgs_backend.model.Usuario;
import com.conectatech.sgs_backend.repository.BitacoraProcedimientoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditoriaService {

    private final BitacoraProcedimientoRepository bitacoraRepository;

    /**
     * Extrae el usuario actual del contexto de seguridad.
     */
    public Usuario getUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Usuario) {
            return (Usuario) auth.getPrincipal();
        }
        return null;
    }

    /**
     * Registro centralizado y unificado de auditoría forense con trazabilidad de incidente y actor explícito.
     */
    public void registrarAuditoria(String incidenteId, String codigoCorrelativo, Usuario actor,
                                   String campoModificado, String valorAnterior, String valorNuevo,
                                   String comentario) {
        // Evitar generar registros basura si los valores son idénticos
        if (Objects.equals(valorAnterior, valorNuevo)) {
            return;
        }

        if (actor == null) {
            actor = getUsuarioActual();
        }

        String usuarioId = actor != null ? actor.getId() : "SISTEMA";
        String nombreActor = actor != null ? actor.getNombreCompleto() : "Sistema";
        String rolActor = actor != null && actor.getRol() != null ? actor.getRol().name() : "SISTEMA";

        // Filtro de auditoría: los eventos de sesión de ciudadanos no se persisten en bitácora operativa
        if (esSesionDeCiudadano(rolActor, campoModificado)) {
            log.debug("Se descarta registro de sesión en bitácora para rol CIUDADANO: {}", nombreActor);
            return;
        }

        BitacoraProcedimiento registro = BitacoraProcedimiento.builder()
                .incidenteId(incidenteId)
                .codigoCorrelativo(codigoCorrelativo)
                .usuarioId(usuarioId)
                .nombreActor(nombreActor)
                .rolActor(rolActor)
                .campoModificado(campoModificado)
                .valorAnterior(valorAnterior)
                .valorNuevo(valorNuevo)
                .comentario(comentario)
                .fechaModificacion(LocalDateTime.now())
                .build();

        bitacoraRepository.save(registro);
        log.info("Auditoría forense registrada: [Campo: {}, Actor: {}, Incidente: {}] - {}",
                campoModificado, nombreActor, codigoCorrelativo != null ? codigoCorrelativo : "N/A", comentario);
    }

    /**
     * Overload con actor extraído automáticamente de SecurityContext.
     */
    public void registrarAuditoria(String incidenteId, String codigoCorrelativo,
                                   String campoModificado, String valorAnterior, String valorNuevo,
                                   String comentario) {
        registrarAuditoria(incidenteId, codigoCorrelativo, getUsuarioActual(), campoModificado, valorAnterior, valorNuevo, comentario);
    }

    /**
     * Overload para eventos de configuración, catálogos o usuarios sin incidente asociado.
     */
    public void registrarAuditoria(String campoModificado, String valorAnterior, String valorNuevo, String comentario) {
        registrarAuditoria(null, null, getUsuarioActual(), campoModificado, valorAnterior, valorNuevo, comentario);
    }

    /**
     * Overload para casos donde el actor se especifica manualmente (p. ej. auto-registro público).
     */
    public void registrarAuditoriaManual(String usuarioId, String nombreActor, String rolActor,
                                         String campoModificado, String valorAnterior, String valorNuevo,
                                         String comentario) {
        if (Objects.equals(valorAnterior, valorNuevo)) {
            return;
        }

        // Filtro de auditoría: los eventos de sesión de ciudadanos no se persisten en bitácora operativa
        if (esSesionDeCiudadano(rolActor, campoModificado)) {
            log.debug("Se descarta registro de sesión en bitácora para rol CIUDADANO: {}", nombreActor);
            return;
        }

        BitacoraProcedimiento registro = BitacoraProcedimiento.builder()
                .usuarioId(usuarioId)
                .nombreActor(nombreActor)
                .rolActor(rolActor)
                .campoModificado(campoModificado)
                .valorAnterior(valorAnterior)
                .valorNuevo(valorNuevo)
                .comentario(comentario)
                .fechaModificacion(LocalDateTime.now())
                .build();

        bitacoraRepository.save(registro);
        log.info("Auditoría forense registrada manualmente: [Campo: {}, Actor: {}] - {}", campoModificado, nombreActor, comentario);
    }

    /**
     * Valida si un evento corresponde a autenticación/sesión (login/logout) y el actor es un ciudadano.
     * En tal caso se descarta para proteger la bitácora operativa de sobrecarga.
     */
    private boolean esSesionDeCiudadano(String rolActor, String campoModificado) {
        if (rolActor == null || campoModificado == null) {
            return false;
        }
        if (!"CIUDADANO".equalsIgnoreCase(rolActor.trim())) {
            return false;
        }
        String campo = campoModificado.toUpperCase().trim();
        return campo.contains("LOGIN") || campo.contains("LOGOUT") || campo.contains("SESION") || campo.contains("SESIÓN");
    }
}
