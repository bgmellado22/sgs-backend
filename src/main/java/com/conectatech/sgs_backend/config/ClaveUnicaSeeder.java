package com.conectatech.sgs_backend.config;

import com.conectatech.sgs_backend.model.Usuario;
import com.conectatech.sgs_backend.model.enums.RolUsuario;
import com.conectatech.sgs_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class ClaveUnicaSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Verificando existencia de identidades oficiales de prueba para ClaveÚnica...");

        asegurarUsuario("44.444.444-4", "Carlos", "Muñoz", "Silva",
                "admin.prueba@eltabo.cl", RolUsuario.ADMINISTRADOR);

        asegurarUsuario("55.555.555-5", "Patricia", "González", "Soto",
                "operador.prueba@eltabo.cl", RolUsuario.OPERADOR);

        asegurarUsuario("88.888.888-8", "Andrés", "Castro", "Riquelme",
                "inspector.prueba@eltabo.cl", RolUsuario.INSPECTOR);

        asegurarUsuario("99.999.999-9", "Claudia", "Sepúlveda", "Mora",
                "ciudadano.prueba@gmail.com", RolUsuario.CIUDADANO);
    }

    private void asegurarUsuario(String run, String nombres, String primerApellido, String segundoApellido,
                                 String email, RolUsuario rol) {
        Optional<Usuario> existente = usuarioRepository.findByRun(run);
        if (existente.isEmpty()) {
            existente = usuarioRepository.findByEmail(email);
        }

        if (existente.isEmpty()) {
            String nombreCompleto = nombres + " " + primerApellido + " " + segundoApellido;
            Usuario usuario = Usuario.builder()
                    .run(run)
                    .nombres(nombres)
                    .primerApellido(primerApellido)
                    .segundoApellido(segundoApellido)
                    .nombreCompleto(nombreCompleto)
                    .email(email)
                    .password(passwordEncoder.encode("testing"))
                    .rol(rol)
                    .estado(true)
                    .intentosFallidos(0)
                    .build();

            usuarioRepository.save(usuario);
            log.info("Identidad de prueba creada para ClaveÚnica: {} [{}] - RUN: {}", nombreCompleto, rol, run);
        }
    }
}

