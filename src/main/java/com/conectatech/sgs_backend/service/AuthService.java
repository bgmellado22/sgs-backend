package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.dto.auth.AuthResponse;
import com.conectatech.sgs_backend.dto.auth.LoginRequest;
import com.conectatech.sgs_backend.dto.auth.RegisterRequest;
import com.conectatech.sgs_backend.exception.CuentaBloqueadaException;
import com.conectatech.sgs_backend.exception.CredencialesIncorrectasException;
import com.conectatech.sgs_backend.model.Usuario;
import com.conectatech.sgs_backend.model.enums.RolUsuario;
import com.conectatech.sgs_backend.repository.UsuarioRepository;
import com.conectatech.sgs_backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

        private static final int MAX_INTENTOS = 3;
        private static final int MINUTOS_BLOQUEO = 15;

        private final UsuarioRepository usuarioRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtUtil jwtUtil;
        private final AuthenticationManager authenticationManager;
        private final AuditoriaService auditoriaService;
        private final ClaveUnicaService claveUnicaService;

        public AuthResponse register(RegisterRequest request) {
                // Validar duplicados antes de guardar (comparación flexible de RUN y correo en minúsculas)
                String emailNormalizado = request.getEmail().trim().toLowerCase();
                if (usuarioRepository.existsByEmail(emailNormalizado)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo ya está registrado.");
                }
                if (claveUnicaService.buscarPorRun(request.getRun()).isPresent()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El RUN ya está registrado en el sistema.");
                }

                // Identificar actor de la petición y validar privilegios (ISO 27001 A.5.15 / A.8.2)
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                boolean esAdmin = auth != null && auth.isAuthenticated() && auth.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));

                // Blindaje contra escalamiento de privilegios:
                // El auto-registro público SIEMPRE asigna RolUsuario.CIUDADANO.
                // Solo un usuario con rol ADMINISTRADOR autenticado puede asignar roles privilegiados.
                RolUsuario rol = (esAdmin && request.getRol() != null) ? request.getRol() : RolUsuario.CIUDADANO;

                String nombres = request.getNombres().trim();
                String primerApellido = request.getPrimerApellido().trim();
                String segundoApellido = request.getSegundoApellido() != null ? request.getSegundoApellido().trim() : "";
                String nombreCompleto = segundoApellido.isEmpty()
                                ? nombres + " " + primerApellido
                                : nombres + " " + primerApellido + " " + segundoApellido;

                // Documento sin contraseña: La autenticación es 100% ClaveÚnica (OIDC)
                var user = Usuario.builder()
                                .run(request.getRun().trim())
                                .nombres(nombres)
                                .primerApellido(primerApellido)
                                .segundoApellido(segundoApellido)
                                .nombreCompleto(nombreCompleto.trim())
                                .email(emailNormalizado)
                                .rol(rol)
                                .estado(true)
                                .intentosFallidos(0)
                                .build();

                usuarioRepository.save(user);

                // Registro en bitácora mediante AuditoriaService
                String actorId = "SISTEMA";
                String nombreActor = "Auto-registro";
                String rolActor = "SISTEMA";
                String comentario = "Nuevo usuario " + user.getNombreCompleto() + " enrolado en padrón institucional (Rol: " + user.getRol().name() + ")";

                if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                        try {
                                Usuario actorAuth = (Usuario) auth.getPrincipal();
                                actorId = actorAuth.getId();
                                nombreActor = actorAuth.getNombreCompleto();
                                rolActor = actorAuth.getRol() != null ? actorAuth.getRol().name() : "ADMINISTRADOR";
                                comentario = "Funcionario " + user.getNombreCompleto() + " enrolado por Administrador " + nombreActor + " (Rol: " + user.getRol().name() + ")";
                        } catch (Exception ignored) {
                        }
                }

                auditoriaService.registrarAuditoriaManual(
                                actorId,
                                nombreActor,
                                rolActor,
                                "USUARIO_REGISTRO",
                                null,
                                "Activo",
                                comentario
                );

                var jwtToken = jwtUtil.generateToken(user);
                return AuthResponse.builder()
                                .token(jwtToken)
                                .rol(user.getRol().name())
                                .build();
        }

        public AuthResponse login(LoginRequest request) {
                var user = usuarioRepository.findByEmail(request.getEmail())
                                .orElseThrow(() -> new CredencialesIncorrectasException("Credenciales incorrectas."));

                verificarBloqueo(user);

                try {
                        authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(request.getEmail(),
                                                        request.getPassword()));
                } catch (AuthenticationException e) {
                        registrarIntentoFallido(user);
                        int restantes = MAX_INTENTOS - user.getIntentosFallidos();
                        throw new CredencialesIncorrectasException(
                                        "Credenciales incorrectas. Intentos restantes: " + restantes);
                }

                if (user.getIntentosFallidos() != null && user.getIntentosFallidos() > 0) {
                        user.setIntentosFallidos(0);
                        user.setBloqueadoHasta(null);
                        usuarioRepository.save(user);
                }

                // Registro forense en bitácora de inicio de sesión exitoso
                try {
                        auditoriaService.registrarAuditoriaManual(
                                        user.getId(),
                                        user.getNombreCompleto(),
                                        user.getRol().name(),
                                        "USUARIO_LOGIN",
                                        "Inactiva",
                                        "Sesión Iniciada",
                                        "Inicio de sesión exitoso mediante correo electrónico (" + user.getEmail() + ")"
                        );
                } catch (Exception e) {
                        // Resiliente: no interrumpir el flujo de autenticación ante contingencias de bitácora
                }

                var jwtToken = jwtUtil.generateToken(user);
                return AuthResponse.builder()
                                .token(jwtToken)
                                .rol(user.getRol().name())
                                .build();
        }

        public void logout() {
                try {
                        Usuario actor = auditoriaService.getUsuarioActual();
                        if (actor != null) {
                                auditoriaService.registrarAuditoriaManual(
                                                actor.getId(),
                                                actor.getNombreCompleto(),
                                                actor.getRol() != null ? actor.getRol().name() : "SISTEMA",
                                                "USUARIO_LOGOUT",
                                                "Sesión Activa",
                                                "Sesión Finalizada",
                                                "Cierre de sesión del usuario " + actor.getNombreCompleto()
                                );
                        }
                } catch (Exception e) {
                        // Resiliente: no interrumpir el flujo ante contingencias de bitácora
                }
        }

        private void verificarBloqueo(Usuario usuario) {
                if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(LocalDateTime.now())) {
                        throw new CuentaBloqueadaException(
                                        "Cuenta bloqueada por múltiples intentos fallidos. Intente en "
                                                        + MINUTOS_BLOQUEO + " minutos.");
                }
        }

        private void registrarIntentoFallido(Usuario usuario) {
                int intentos = (usuario.getIntentosFallidos() == null ? 0 : usuario.getIntentosFallidos()) + 1;
                usuario.setIntentosFallidos(intentos);

                if (intentos >= MAX_INTENTOS) {
                        usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEO));
                }

                usuarioRepository.save(usuario);
        }
}
