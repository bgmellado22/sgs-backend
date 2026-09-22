package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.dto.auth.AuthResponse;
import com.conectatech.sgs_backend.dto.auth.LoginRequest;
import com.conectatech.sgs_backend.dto.auth.RegisterRequest;
import com.conectatech.sgs_backend.exception.CuentaBloqueadaException;
import com.conectatech.sgs_backend.exception.CredencialesIncorrectasException;
import com.conectatech.sgs_backend.model.BitacoraProcedimiento;
import com.conectatech.sgs_backend.model.Usuario;
import com.conectatech.sgs_backend.repository.BitacoraProcedimientoRepository;
import com.conectatech.sgs_backend.repository.UsuarioRepository;
import com.conectatech.sgs_backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
        private final BitacoraProcedimientoRepository bitacoraRepository;

        public AuthResponse register(RegisterRequest request) {
                var user = Usuario.builder()
                                .rut(request.getRut())
                                .nombreCompleto(request.getNombreCompleto())
                                .email(request.getEmail())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .rol(request.getRol())
                                .estado(true)
                                .intentosFallidos(0)
                                .build();

                usuarioRepository.save(user);

                // Registro en bitácora
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String actorId = "SISTEMA";
                String nombreActor = "Auto-registro";
                String rolActor = "SISTEMA";
                String comentario = "Nuevo usuario " + user.getNombreCompleto() + " registrado por auto-registro";

                if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                        try {
                                Usuario actorAuth = (Usuario) auth.getPrincipal();
                                actorId = actorAuth.getId();
                                nombreActor = actorAuth.getNombreCompleto();
                                rolActor = actorAuth.getRol().name();
                                comentario = "Nuevo usuario " + user.getNombreCompleto() + " registrado por " + nombreActor;
                        } catch (Exception e) {
                                // Fallback si el principal no es del tipo Usuario
                        }
                }

                BitacoraProcedimiento registro = BitacoraProcedimiento.builder()
                                .incidenteId(null)
                                .usuarioId(actorId)
                                .nombreActor(nombreActor)
                                .rolActor(rolActor)
                                .campoModificado("Registro Usuario")
                                .valorAnterior(null)
                                .valorNuevo("Activo")
                                .comentario(comentario)
                                .fechaModificacion(LocalDateTime.now())
                                .build();

                bitacoraRepository.save(registro);

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

                var jwtToken = jwtUtil.generateToken(user);
                return AuthResponse.builder()
                                .token(jwtToken)
                                .rol(user.getRol().name())
                                .build();
        }

        // Sudo mode
        public void verificarSudo(String emailAdmin, String rawPassword) {
                Usuario admin = usuarioRepository.findByEmail(emailAdmin)
                                .orElseThrow(() -> new CredencialesIncorrectasException("Credenciales incorrectas."));

                verificarBloqueo(admin);

                if (passwordEncoder.matches(rawPassword, admin.getPassword())) {
                        admin.setIntentosFallidos(0);
                        admin.setBloqueadoHasta(null);
                        usuarioRepository.save(admin);
                } else {
                        registrarIntentoFallido(admin);
                        int restantes = MAX_INTENTOS - admin.getIntentosFallidos();
                        throw new CredencialesIncorrectasException(
                                        "Contraseña incorrecta. Intentos restantes: " + restantes);
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