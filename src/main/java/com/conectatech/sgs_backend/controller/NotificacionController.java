package com.conectatech.sgs_backend.controller;

import com.conectatech.sgs_backend.model.Notificacion;
import com.conectatech.sgs_backend.model.Usuario;
import com.conectatech.sgs_backend.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notificaciones")
@RequiredArgsConstructor
@CrossOrigin(origins = { "http://localhost:5173", "https://sgs-el-tabo-frontend.vercel.app" })
public class NotificacionController {
    private final NotificacionService notificacionService;

    // Obtener todas las notificaciones del usuario autenticado
    @GetMapping
    public ResponseEntity<List<Notificacion>> obtenerMisNotificaciones(
            @AuthenticationPrincipal Usuario usuarioAuth) {
        return ResponseEntity.ok(notificacionService.obtenerNotificacionesPorUsuario(usuarioAuth.getId()));
    }

    // Obtener el contador de mensajes sin leer para el badge rojo de la campana
    @GetMapping("/no-leidas/count")
    public ResponseEntity<Long> contarNoLeidas(
            @AuthenticationPrincipal Usuario usuarioAuth) {
        return ResponseEntity.ok(notificacionService.contarNoLeidas(usuarioAuth.getId()));
    }

    // Marcar una notificación como leída
    @PatchMapping("/{id}/leida")
    public ResponseEntity<Void> marcarComoLeida(@PathVariable("id") String id) {
        notificacionService.marcarComoLeida(id);
        return ResponseEntity.noContent().build();
    }

    // Marcar todas las notificaciones como leídas
    @PatchMapping("/leidas")
    public ResponseEntity<Void> marcarTodasComoLeidas(
            @AuthenticationPrincipal Usuario usuarioAuth) {
        notificacionService.marcarTodasComoLeidas(usuarioAuth.getId());
        return ResponseEntity.noContent().build();
    }

    // Endpoint de prueba
    @PostMapping
    public ResponseEntity<Notificacion> crearNotificacionPrueba(@RequestBody Notificacion notificacion) {
        // En el flujo real del SGS, las notificaciones nacerán internamente en otros
        // servicios
        // (ej. cuando IncidenteService actualiza un estado), pero esto te permite
        // probar desde Postman.
        Notificacion creada = notificacionService.crearNotificacion(notificacion);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }
}
