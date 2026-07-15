package com.conectatech.sgs_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendPasswordResetEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("SGS El Tabo - Recuperación de Contraseña");

        String resetUrl = "http://localhost:5173/recuperar-clave?token=" + token;

        message.setText(
                "Hola,\n\nHas solicitado restablecer tu contraseña para el Sistema de Gestión de Seguridad (SGS).\n\n" +
                        "Haz clic en el siguiente enlace para crear una nueva contraseña:\n" +
                        resetUrl + "\n\n" +
                        "Este enlace expirará en 15 minutos por motivos de seguridad.\n" +
                        "Si no solicitaste esto, puedes ignorar este correo.");

        mailSender.send(message);
    }
}
