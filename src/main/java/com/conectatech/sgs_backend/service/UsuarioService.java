package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.dto.MiPerfilUpdateDTO;
import com.conectatech.sgs_backend.dto.UsuarioResponseDTO;
import com.conectatech.sgs_backend.dto.UsuarioUpdateDTO;
import com.conectatech.sgs_backend.model.Usuario;
import com.conectatech.sgs_backend.model.enums.RolUsuario;
import com.conectatech.sgs_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

        private final UsuarioRepository usuarioRepository;
        private final AuditoriaService auditoriaService;
        private final PasswordEncoder passwordEncoder;

        // Obtener usuarios
        public List<UsuarioResponseDTO> obtenerTodosLosUsuarios() {
                List<Usuario> usuarios = usuarioRepository.findAll();

                return usuarios.stream()
                                .map(this::mapToDTO)
                                .collect(Collectors.toList());
        }

        // Actualizar usuario
        public UsuarioResponseDTO actualizarUsuario(String id, UsuarioUpdateDTO dto) {
                Usuario usuario = usuarioRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado con ID: " + id));

                RolUsuario rolAnterior = usuario.getRol();
                String emailAnterior = usuario.getEmail();

                if (dto.getNombres() != null) {
                        usuario.setNombres(dto.getNombres());
                }
                if (dto.getPrimerApellido() != null) {
                        usuario.setPrimerApellido(dto.getPrimerApellido());
                }
                if (dto.getSegundoApellido() != null) {
                        usuario.setSegundoApellido(dto.getSegundoApellido());
                }
                if (dto.getNombres() != null && dto.getPrimerApellido() != null) {
                        String seg = dto.getSegundoApellido() != null && !dto.getSegundoApellido().isBlank()
                                        ? " " + dto.getSegundoApellido().trim()
                                        : "";
                        usuario.setNombreCompleto(dto.getNombres().trim() + " " + dto.getPrimerApellido().trim() + seg);
                }

                usuario.setEmail(dto.getEmail());
                usuario.setRol(dto.getRol());

                Usuario usuarioActualizado = usuarioRepository.save(usuario);

                // Registro de auditoría unificado
                String valorAnt = String.format("Rol: %s, Email: %s", rolAnterior, emailAnterior);
                String valorNue = String.format("Rol: %s, Email: %s", usuarioActualizado.getRol(), usuarioActualizado.getEmail());
                auditoriaService.registrarAuditoria("USUARIO_MODIFICACION", valorAnt, valorNue,
                                "Edición de usuario " + usuarioActualizado.getNombreCompleto());

                return mapToDTO(usuarioActualizado);
        }

        // Actualizar mi propio perfil
        public UsuarioResponseDTO actualizarMiPerfil(MiPerfilUpdateDTO dto) {
                Usuario actor = auditoriaService.getUsuarioActual();
                if (actor == null) {
                        throw new IllegalArgumentException("No hay una sesión activa para actualizar el perfil.");
                }

                Usuario usuario = usuarioRepository.findById(actor.getId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Usuario no encontrado con ID: " + actor.getId()));

                if (dto.getNombres() != null) {
                        usuario.setNombres(dto.getNombres());
                }
                if (dto.getPrimerApellido() != null) {
                        usuario.setPrimerApellido(dto.getPrimerApellido());
                }
                if (dto.getSegundoApellido() != null) {
                        usuario.setSegundoApellido(dto.getSegundoApellido());
                }
                if (dto.getNombres() != null && dto.getPrimerApellido() != null) {
                        String seg = dto.getSegundoApellido() != null && !dto.getSegundoApellido().isBlank()
                                        ? " " + dto.getSegundoApellido().trim()
                                        : "";
                        usuario.setNombreCompleto(dto.getNombres().trim() + " " + dto.getPrimerApellido().trim() + seg);
                }
                if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
                        usuario.setEmail(dto.getEmail());
                }
                if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
                        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
                }

                Usuario usuarioActualizado = usuarioRepository.save(usuario);

                return mapToDTO(usuarioActualizado);
        }

        // Cambiar estado del usuario
        public UsuarioResponseDTO cambiarEstadoUsuario(String id) {
                Usuario usuario = usuarioRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado con ID: " + id));

                boolean estadoActual = Boolean.TRUE.equals(usuario.getEstado());
                String estadoAnterior = estadoActual ? "Activo" : "Inactivo";
                usuario.setEstado(!estadoActual);
                String estadoNuevo = !estadoActual ? "Activo" : "Inactivo";

                Usuario usuarioActualizado = usuarioRepository.save(usuario);

                // Registro en bitácora unificado
                auditoriaService.registrarAuditoria(
                                "USUARIO_ESTADO",
                                estadoAnterior,
                                estadoNuevo,
                                "Usuario " + usuario.getNombreCompleto() + " fue " + estadoNuevo.toLowerCase()
                );

                return mapToDTO(usuarioActualizado);
        }

        // Obtener inspectores activos (para el mapa operativo)
        public List<UsuarioResponseDTO> obtenerInspectoresActivos() {
                List<Usuario> inspectores = usuarioRepository.findByRolAndEstado(RolUsuario.INSPECTOR, true);

                return inspectores.stream()
                                .map(this::mapToDTO)
                                .collect(Collectors.toList());
        }

        private UsuarioResponseDTO mapToDTO(Usuario usuario) {
                return UsuarioResponseDTO.builder()
                                .id(usuario.getId())
                                .run(usuario.getRun())
                                .nombres(usuario.getNombres())
                                .primerApellido(usuario.getPrimerApellido())
                                .segundoApellido(usuario.getSegundoApellido())
                                .nombreCompleto(usuario.getNombreCompleto())
                                .email(usuario.getEmail())
                                .rol(usuario.getRol())
                                .estado(usuario.getEstado())
                                .ultimoAcceso(usuario.getUltimoAcceso())
                                .build();
        }
}
