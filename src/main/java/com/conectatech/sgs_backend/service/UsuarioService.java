package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.dto.UsuarioResponseDTO;
import com.conectatech.sgs_backend.dto.UsuarioUpdateDTO;
import com.conectatech.sgs_backend.model.Usuario;
import com.conectatech.sgs_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

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
}