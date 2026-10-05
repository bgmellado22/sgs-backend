package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.model.BitacoraProcedimiento;
import com.conectatech.sgs_backend.model.Usuario;
import com.conectatech.sgs_backend.repository.BitacoraProcedimientoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditoriaService {

    private final BitacoraProcedimientoRepository bitacoraRepository;

    /**
     * Extrae el usuario actual del contexto de seguridad.
     */
    public Usuario getUsuarioActual() {
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
            SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof Usuario) {
            return (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        }
        return null;
    }

    /**
     * Registra un evento en la bitácora de auditoría.
     */
    public void registrarAuditoria(String campoModificado, String valorAnterior, String valorNuevo, String comentario) {
        Usuario actor = getUsuarioActual();
        
        if (actor == null) {
            log.warn("No se pudo registrar en auditoría: No hay un usuario autenticado en el contexto.");
            return;
        }

        BitacoraProcedimiento registro = BitacoraProcedimiento.builder()
                .usuarioId(actor.getId())
                .nombreActor(actor.getNombreCompleto())
                .rolActor(actor.getRol().name())
                .campoModificado(campoModificado)
                .valorAnterior(valorAnterior)
                .valorNuevo(valorNuevo)
                .comentario(comentario)
                .fechaModificacion(LocalDateTime.now())
                .build();

        bitacoraRepository.save(registro);
        log.info("Auditoría forense registrada: {}", comentario);
    }
}
