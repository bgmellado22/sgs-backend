package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.dto.UsuarioResponseDTO;
import com.conectatech.sgs_backend.dto.UsuarioUpdateDTO;
import com.conectatech.sgs_backend.model.BitacoraProcedimiento;
import com.conectatech.sgs_backend.model.Usuario;
import com.conectatech.sgs_backend.model.enums.RolUsuario;
import com.conectatech.sgs_backend.repository.BitacoraProcedimientoRepository;
import com.conectatech.sgs_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

        private final UsuarioRepository usuarioRepository;
        private final BitacoraProcedimientoRepository bitacoraRepository;

        // Obtener usuarios
        public List<UsuarioResponseDTO> obtenerTodosLosUsuarios() {
                List<Usuario> usuarios = usuarioRepository.findAll();

                return usuarios.stream()
                                .map(usuario -> UsuarioResponseDTO.builder()
                                                .id(usuario.getId())
                                                .rut(usuario.getRut())
                                                .nombreCompleto(usuario.getNombreCompleto())
                                                .email(usuario.getEmail())
                                                .rol(usuario.getRol())
                                                .estado(usuario.getEstado())
                                                .build())
                                .collect(Collectors.toList());
        }

        // Actualizar usuario
        public UsuarioResponseDTO actualizarUsuario(String id, UsuarioUpdateDTO dto) {
                Usuario usuario = usuarioRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

                usuario.setNombreCompleto(dto.getNombreCompleto());
                usuario.setEmail(dto.getEmail());
                usuario.setRol(dto.getRol());

                Usuario usuarioActualizado = usuarioRepository.save(usuario);

                return UsuarioResponseDTO.builder()
                                .id(usuarioActualizado.getId())
                                .rut(usuarioActualizado.getRut())
                                .nombreCompleto(usuarioActualizado.getNombreCompleto())
                                .email(usuarioActualizado.getEmail())
                                .rol(usuarioActualizado.getRol())
                                .estado(usuarioActualizado.getEstado())
                                .build();
        }

        // Cambiar estado del usuario
        public UsuarioResponseDTO cambiarEstadoUsuario(String id) {
                Usuario usuario = usuarioRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

                String estadoAnterior = Boolean.TRUE.equals(usuario.getEstado()) ? "Activo" : "Inactivo";
                usuario.setEstado(!usuario.getEstado());
                String estadoNuevo = Boolean.TRUE.equals(usuario.getEstado()) ? "Activo" : "Inactivo";

                Usuario usuarioActualizado = usuarioRepository.save(usuario);

                // Registro en bitácora
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                Usuario actor = (Usuario) auth.getPrincipal();

                BitacoraProcedimiento registro = BitacoraProcedimiento.builder()
                                .incidenteId(null)
                                .usuarioId(actor.getId())
                                .nombreActor(actor.getNombreCompleto())
                                .rolActor(actor.getRol().name())
                                .campoModificado("Estado Usuario")
                                .valorAnterior(estadoAnterior)
                                .valorNuevo(estadoNuevo)
                                .comentario("Usuario " + usuario.getNombreCompleto() + " fue " + estadoNuevo.toLowerCase() + " por " + actor.getNombreCompleto())
                                .fechaModificacion(LocalDateTime.now())
                                .build();

                bitacoraRepository.save(registro);

                return UsuarioResponseDTO.builder()
                                .id(usuarioActualizado.getId())
                                .rut(usuarioActualizado.getRut())
                                .nombreCompleto(usuarioActualizado.getNombreCompleto())
                                .email(usuarioActualizado.getEmail())
                                .rol(usuarioActualizado.getRol())
                                .estado(usuarioActualizado.getEstado())
                                .build();
        }

        // Obtener inspectores activos (para el mapa operativo)
        public List<UsuarioResponseDTO> obtenerInspectoresActivos() {
                List<Usuario> inspectores = usuarioRepository.findByRolAndEstado(RolUsuario.INSPECTOR, true);

                return inspectores.stream()
                                .map(inspector -> UsuarioResponseDTO.builder()
                                                .id(inspector.getId())
                                                .rut(inspector.getRut())
                                                .nombreCompleto(inspector.getNombreCompleto())
                                                .email(inspector.getEmail())
                                                .rol(inspector.getRol())
                                                .estado(inspector.getEstado())
                                                .build())
                                .collect(Collectors.toList());
        }
}