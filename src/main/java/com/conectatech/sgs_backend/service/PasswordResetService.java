package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.model.PasswordResetToken;
import com.conectatech.sgs_backend.model.Usuario;
import com.conectatech.sgs_backend.repository.PasswordResetTokenRepository;
import com.conectatech.sgs_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public void processForgotPassword(String email) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        // Si no existe, salimos silenciosamente
        if (usuarioOpt.isEmpty()) {
            return;
        }

        Usuario usuario = usuarioOpt.get();

        // Eliminar un token anterior si es que el usuario solicitó otro hace poco
        tokenRepository.findByUsuarioId(usuario.getId())
                .ifPresent(tokenRepository::delete);

        // Generar nuevo token único
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .usuarioId(usuario.getId())
                .fechaExpiracion(LocalDateTime.now().plusMinutes(15)) // Expira en 15 min
                .build();

        tokenRepository.save(resetToken);

        // Disparar el correo
        emailService.sendPasswordResetEmail(usuario.getEmail(), token);
    }

    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();

        // Validar si el token ya expiró
        if (resetToken.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(resetToken); // Limpiamos la BD
            return false;
        }

        // Buscar al usuario dueño de ese token
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(resetToken.getUsuarioId());
        if (usuarioOpt.isEmpty()) {
            return false;
        }

        // Encriptar y guardar la nueva contraseña
        Usuario usuario = usuarioOpt.get();
        usuario.setPassword(passwordEncoder.encode(newPassword));
        usuarioRepository.save(usuario);

        // Borrar el token para que no se pueda volver a usar
        tokenRepository.delete(resetToken);
        return true;
    }
}
