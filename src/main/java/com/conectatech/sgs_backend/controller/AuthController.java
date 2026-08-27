package com.conectatech.sgs_backend.controller;

import com.conectatech.sgs_backend.dto.SudoRequestDTO;
import com.conectatech.sgs_backend.dto.auth.AuthResponse;
import com.conectatech.sgs_backend.dto.auth.LoginRequest;
import com.conectatech.sgs_backend.dto.auth.RegisterRequest;
import com.conectatech.sgs_backend.dto.auth.ForgotPasswordRequest;
import com.conectatech.sgs_backend.dto.auth.ResetPasswordRequest;
import com.conectatech.sgs_backend.service.AuthService;
import com.conectatech.sgs_backend.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = { "http://localhost:5173", "https://sgs-el-tabo-frontend.vercel.app" })
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        passwordResetService.processForgotPassword(request.getEmail());
        // Siempre devolvemos 200 OK por seguridad contra ataques de enumeración
        return ResponseEntity.ok("Si el correo existe, se ha enviado un enlace de recuperación.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        boolean success = passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        if (success) {
            return ResponseEntity.ok("Contraseña actualizada correctamente.");
        } else {
            return ResponseEntity.badRequest().body("Token inválido o expirado.");
        }
    }

    @PostMapping("/sudo")
    public ResponseEntity<?> validarSudoMode(@RequestBody SudoRequestDTO request, Authentication authentication) {
        authService.verificarSudo(authentication.getName(), request.getPassword());
        return ResponseEntity.ok().build();
    }
}
