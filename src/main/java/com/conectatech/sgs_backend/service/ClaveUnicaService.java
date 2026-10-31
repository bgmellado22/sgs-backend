package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.dto.auth.AuthResponse;
import com.conectatech.sgs_backend.dto.claveunica.ClaveUnicaCallbackRequest;
import com.conectatech.sgs_backend.dto.claveunica.ClaveUnicaTokenResponse;
import com.conectatech.sgs_backend.dto.claveunica.ClaveUnicaUserInfoDTO;
import com.conectatech.sgs_backend.model.Usuario;
import com.conectatech.sgs_backend.model.enums.RolUsuario;
import com.conectatech.sgs_backend.repository.UsuarioRepository;
import com.conectatech.sgs_backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaveUnicaService {

    private final UsuarioRepository usuarioRepository;
    private final JwtUtil jwtUtil;
    private final AuditoriaService auditoriaService;
    private final PasswordEncoder passwordEncoder;

    @Value("${claveunica.mode:mock}")
    private String mode;

    @Value("${claveunica.client-id:mock_el_tabo_client_id}")
    private String clientId;

    @Value("${claveunica.client-secret:mock_el_tabo_client_secret}")
    private String clientSecret;

    @Value("${claveunica.redirect-uri:http://localhost:5173/auth/claveunica/callback}")
    private String redirectUri;

    @Value("${claveunica.token-url:https://accounts.claveunica.gob.cl/openid/token/}")
    private String tokenUrl;

    @Value("${claveunica.userinfo-url:https://accounts.claveunica.gob.cl/openid/userinfo/}")
    private String userinfoUrl;

    /**
     * Procesa la solicitud de login tras el callback de ClaveÚnica.
     */
    public AuthResponse procesarLoginClaveUnica(ClaveUnicaCallbackRequest request) {
        log.info("Iniciando procesamiento de login ClaveÚnica [Modo: {}, Canal: {}]", mode, request.getCanal());

        ClaveUnicaUserInfoDTO userInfo = "real".equalsIgnoreCase(mode)
                ? resolverUserInfoReal(request.getCode())
                : resolverUserInfoMock(request.getCode());

        String runExtraido = userInfo.getRunFormateado();
        log.info("Identidad resuelta desde ClaveÚnica para RUN: {}", runExtraido);

        Optional<Usuario> usuarioOpt = buscarPorRun(runExtraido);

        // Si no existe en la base de datos
        if (usuarioOpt.isEmpty()) {
            if ("WEB".equalsIgnoreCase(request.getCanal())) {
                log.warn("Acceso denegado: RUN {} no registrado previamente como personal municipal.", runExtraido);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "El RUN " + runExtraido + " no figura registrado como personal municipal. " +
                        "Esta plataforma web está destinada exclusivamente para la administración y operación central.");
            } else {
                // Auto-registro para el canal móvil "Más Seguridad"
                Usuario nuevoCiudadano = autoRegistrarCiudadano(runExtraido, userInfo);
                return generarRespuestaAuth(nuevoCiudadano);
            }
        }

        Usuario usuario = usuarioOpt.get();

        if (!Boolean.TRUE.equals(usuario.getEstado())) {
            log.warn("Intento de acceso con cuenta desactivada: {}", usuario.getEmail());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tu cuenta de usuario se encuentra desactivada. Contacta al Administrador municipal.");
        }

        // Control de Acceso por Canal (RBAC Estricto)
        if ("WEB".equalsIgnoreCase(request.getCanal())) {
            if (usuario.getRol() == RolUsuario.INSPECTOR) {
                log.warn("Inspector {} intentó ingresar a la plataforma Web.", usuario.getRun());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Acceso restringido: Esta plataforma web está reservada para personal administrativo central. " +
                        "Como Inspector de Terreno, por favor utiliza la aplicación móvil 'Más Seguridad'.");
            }
            if (usuario.getRol() == RolUsuario.CIUDADANO) {
                log.warn("Ciudadano {} intentó ingresar a la plataforma Web.", usuario.getRun());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Acceso exclusivo municipal: Esta plataforma web es de uso exclusivo para personal administrativo y operativo. " +
                        "Para realizar reportes vecinales o consultar incidentes, por favor utiliza la aplicación móvil 'Más Seguridad'.");
            }
        }

        // Registrar auditoría de inicio de sesión exitoso por ClaveÚnica
        try {
            auditoriaService.registrarAuditoriaManual(
                    usuario.getId(),
                    usuario.getNombreCompleto(),
                    usuario.getRol().name(),
                    "LOGIN_CLAVEUNICA",
                    null,
                    "Exitoso",
                    "Inicio de sesión exitoso mediante ClaveÚnica (RUN: " + usuario.getRun() + ") en canal " + request.getCanal()
            );
        } catch (Exception e) {
            log.warn("No se pudo registrar la auditoría de login para {}: {}", usuario.getRun(), e.getMessage());
        }

        return generarRespuestaAuth(usuario);
    }

    /**
     * Búsqueda flexible de usuario por RUN (con puntos, sin puntos, con o sin guion).
     */
    public Optional<Usuario> buscarPorRun(String run) {
        if (run == null || run.trim().isEmpty()) return Optional.empty();

        String limpio = run.replaceAll("[.\\-\\s]", "").toUpperCase();

        // 1. Intento exacto
        Optional<Usuario> u = usuarioRepository.findByRun(run);
        if (u.isPresent()) return u;

        // 2. Intento formato con puntos y guion (ej. 44.444.444-4)
        String conPuntosYGuion = formatearRutConPuntos(limpio);
        u = usuarioRepository.findByRun(conPuntosYGuion);
        if (u.isPresent()) return u;

        // 3. Intento formato sólo con guion (ej. 44444444-4)
        if (limpio.length() > 1) {
            String conGuion = limpio.substring(0, limpio.length() - 1) + "-" + limpio.substring(limpio.length() - 1);
            u = usuarioRepository.findByRun(conGuion);
            if (u.isPresent()) return u;
        }

        // 4. Intento limpio
        return usuarioRepository.findByRun(limpio);
    }

    private String formatearRutConPuntos(String limpio) {
        if (limpio.length() <= 1) return limpio;
        String cuerpo = limpio.substring(0, limpio.length() - 1);
        String dv = limpio.substring(limpio.length() - 1);
        StringBuilder formatted = new StringBuilder();
        int len = cuerpo.length();
        for (int i = 0; i < len; i++) {
            if (i > 0 && (len - i) % 3 == 0) {
                formatted.append(".");
            }
            formatted.append(cuerpo.charAt(i));
        }
        formatted.append("-").append(dv);
        return formatted.toString();
    }

    /**
     * Resolución en modo Mock (Sandbox Interno).
     */
    private ClaveUnicaUserInfoDTO resolverUserInfoMock(String code) {
        String runNormalizado = "44.444.444-4"; // Default
        if (code != null) {
            String c = code.replace("CODE_", "").replaceAll("[.\\-\\s]", "").toUpperCase();
            if (c.startsWith("44444444")) runNormalizado = "44.444.444-4";
            else if (c.startsWith("55555555")) runNormalizado = "55.555.555-5";
            else if (c.startsWith("88888888")) runNormalizado = "88.888.888-8";
            else if (c.startsWith("99999999")) runNormalizado = "99.999.999-9";
            else if (c.length() >= 7) {
                runNormalizado = formatearRutConPuntos(c);
            }
        }

        String dv = runNormalizado.substring(runNormalizado.length() - 1);
        int numero = Integer.parseInt(runNormalizado.substring(0, runNormalizado.length() - 2).replace(".", ""));

        List<String> nombres;
        List<String> apellidos;

        switch (runNormalizado) {
            case "44.444.444-4":
                nombres = List.of("Carlos", "Eduardo");
                apellidos = List.of("Muñoz", "Silva");
                break;
            case "55.555.555-5":
                nombres = List.of("Patricia", "Andrea");
                apellidos = List.of("González", "Soto");
                break;
            case "88.888.888-8":
                nombres = List.of("Andrés", "Ignacio");
                apellidos = List.of("Castro", "Riquelme");
                break;
            case "99.999.999-9":
                nombres = List.of("Claudia", "Lorena");
                apellidos = List.of("Sepúlveda", "Mora");
                break;
            default:
                nombres = List.of("Usuario", "Prueba");
                apellidos = List.of("Vecino", "El Tabo");
                break;
        }

        return ClaveUnicaUserInfoDTO.builder()
                .sub(UUID.randomUUID().toString().substring(0, 8))
                .rolUnico(ClaveUnicaUserInfoDTO.RolUnicoDTO.builder()
                        .numero(numero)
                        .dv(dv)
                        .tipo("RUN")
                        .build())
                .name(ClaveUnicaUserInfoDTO.NameDTO.builder()
                        .nombres(nombres)
                        .apellidos(apellidos)
                        .build())
                .build();
    }

    /**
     * Resolución en modo Real contra los servidores de Gobierno Digital.
     */
    private ClaveUnicaUserInfoDTO resolverUserInfoReal(String code) {
        RestTemplate restTemplate = new RestTemplate();

        // 1. Intercambio de authorization_code por Access Token
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");
        body.add("code", code);

        HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<ClaveUnicaTokenResponse> tokenResponse = restTemplate.postForEntity(
                    tokenUrl, tokenRequest, ClaveUnicaTokenResponse.class);

            if (!tokenResponse.getStatusCode().is2xxSuccessful() || tokenResponse.getBody() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo obtener el token de acceso de ClaveÚnica.");
            }

            String accessToken = tokenResponse.getBody().getAccessToken();

            // 2. Consulta de información de usuario con Bearer token
            HttpHeaders userinfoHeaders = new HttpHeaders();
            userinfoHeaders.setBearerAuth(accessToken);
            HttpEntity<Void> userinfoRequest = new HttpEntity<>(userinfoHeaders);

            ResponseEntity<ClaveUnicaUserInfoDTO> userinfoResponse = restTemplate.exchange(
                    userinfoUrl, HttpMethod.POST, userinfoRequest, ClaveUnicaUserInfoDTO.class);

            if (!userinfoResponse.getStatusCode().is2xxSuccessful() || userinfoResponse.getBody() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo obtener los datos del ciudadano desde ClaveÚnica.");
            }

            return userinfoResponse.getBody();
        } catch (Exception e) {
            log.error("Error al comunicarse con ClaveÚnica de Gobierno Digital: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error de comunicación con ClaveÚnica: " + e.getMessage());
        }
    }

    private Usuario autoRegistrarCiudadano(String run, ClaveUnicaUserInfoDTO userInfo) {
        String nombres = userInfo.getNombresString();
        String primer = userInfo.getPrimerApellido();
        String segundo = userInfo.getSegundoApellido();
        String nombreCompleto = userInfo.getNombreCompleto();

        String emailGenerado = "ciudadano." + run.replaceAll("[^0-9kK]", "") + "@claveunica.gob.cl";

        Usuario nuevo = Usuario.builder()
                .run(run)
                .nombres(nombres)
                .primerApellido(primer)
                .segundoApellido(segundo)
                .nombreCompleto(nombreCompleto)
                .email(emailGenerado)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .rol(RolUsuario.CIUDADANO)
                .estado(true)
                .intentosFallidos(0)
                .build();

        usuarioRepository.save(nuevo);
        log.info("Nuevo ciudadano auto-registrado desde ClaveÚnica móvil: {} (RUN: {})", nombreCompleto, run);
        return nuevo;
    }

    private AuthResponse generarRespuestaAuth(Usuario usuario) {
        String token = jwtUtil.generateToken(usuario);
        return AuthResponse.builder()
                .token(token)
                .rol(usuario.getRol().name())
                .build();
    }
}

