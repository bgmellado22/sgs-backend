package com.conectatech.sgs_backend.controller;

import com.conectatech.sgs_backend.dto.UsuarioResponseDTO;
import com.conectatech.sgs_backend.dto.UsuarioUpdateDTO;
import com.conectatech.sgs_backend.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
@CrossOrigin(origins = { "http://localhost:5173", "https://sgs-el-tabo-frontend.vercel.app" })
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping("/inspectores")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR', 'INSPECTOR')")
    public ResponseEntity<List<UsuarioResponseDTO>> listarInspectores() {
        List<UsuarioResponseDTO> inspectores = usuarioService.obtenerInspectoresActivos();
        return ResponseEntity.ok(inspectores);
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> obtenerUsuarios() {
        return ResponseEntity.ok(usuarioService.obtenerTodosLosUsuarios());
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> actualizarUsuario(
            @PathVariable String id,
            @jakarta.validation.Valid @RequestBody UsuarioUpdateDTO updateDTO) {

        UsuarioResponseDTO usuarioActualizado = usuarioService.actualizarUsuario(id, updateDTO);
        return ResponseEntity.ok(usuarioActualizado);
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<UsuarioResponseDTO> cambiarEstadoUsuario(
            @PathVariable String id,
            @RequestBody(required = false) java.util.Map<String, Object> body,
            @RequestParam(value = "motivo", required = false) String motivoParam) {

        String motivo = motivoParam;
        if ((motivo == null || motivo.isBlank()) && body != null && body.containsKey("motivo")) {
            Object val = body.get("motivo");
            if (val != null) {
                motivo = val.toString();
            }
        }

        UsuarioResponseDTO usuarioActualizado = usuarioService.cambiarEstadoUsuario(id, motivo);
        return ResponseEntity.ok(usuarioActualizado);
    }

    @PutMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> actualizarMiPerfil(@jakarta.validation.Valid @RequestBody com.conectatech.sgs_backend.dto.MiPerfilUpdateDTO updateDTO) {
        UsuarioResponseDTO usuarioActualizado = usuarioService.actualizarMiPerfil(updateDTO);
        return ResponseEntity.ok(usuarioActualizado);
    }
}
