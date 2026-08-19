package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.dto.UsuarioResponseDTO;
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
}