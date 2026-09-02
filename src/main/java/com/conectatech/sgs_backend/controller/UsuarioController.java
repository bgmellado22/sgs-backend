package com.conectatech.sgs_backend.controller;

import com.conectatech.sgs_backend.dto.UsuarioResponseDTO;
import com.conectatech.sgs_backend.dto.UsuarioUpdateDTO;
import com.conectatech.sgs_backend.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
@CrossOrigin(origins = { "http://localhost:5173", "https://sgs-el-tabo-frontend.vercel.app" })
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> obtenerUsuarios() {
        return ResponseEntity.ok(usuarioService.obtenerTodosLosUsuarios());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> actualizarUsuario(
            @PathVariable String id,
            @RequestBody UsuarioUpdateDTO updateDTO) {

        UsuarioResponseDTO usuarioActualizado = usuarioService.actualizarUsuario(id, updateDTO);
        return ResponseEntity.ok(usuarioActualizado);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<UsuarioResponseDTO> cambiarEstadoUsuario(@PathVariable String id) {
        UsuarioResponseDTO usuarioActualizado = usuarioService.cambiarEstadoUsuario(id);
        return ResponseEntity.ok(usuarioActualizado);
    }
}