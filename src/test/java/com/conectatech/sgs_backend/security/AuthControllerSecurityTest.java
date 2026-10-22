package com.conectatech.sgs_backend.security;

import com.conectatech.sgs_backend.controller.AuthController;
import com.conectatech.sgs_backend.dto.auth.AuthResponse;
import com.conectatech.sgs_backend.dto.auth.LoginRequest;
import com.conectatech.sgs_backend.dto.auth.RegisterRequest;
import com.conectatech.sgs_backend.model.enums.RolUsuario;
import com.conectatech.sgs_backend.service.AuthService;
import com.conectatech.sgs_backend.service.PasswordResetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
public class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /register con datos válidos y contraseña robusta → 200 OK")
    public void testRegistro_DatosValidos_Retorna200() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .run("11.111.111-1")
                .nombres("Juan Carlos")
                .primerApellido("Pérez")
                .email("juan.perez@eltabo.cl")
                .password("ClaveSegura#2026")
                .rol(RolUsuario.CIUDADANO)
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .token("mock-token-jwt")
                .rol("CIUDADANO")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-token-jwt"))
                .andExpect(jsonPath("$.rol").value("CIUDADANO"));
    }

    @Test
    @DisplayName("POST /register con contraseña débil (< 8 chars o sin números/símbolos) → 400 Bad Request")
    public void testRegistro_PasswordDebil_Retorna400() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .run("11.111.111-1")
                .nombres("Juan")
                .primerApellido("Pérez")
                .email("juan@eltabo.cl")
                .password("12345") // Débil: sin mayúscula, minúscula, símbolo ni longitud mínima
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").exists());
    }

    @Test
    @DisplayName("POST /register con RUN chileno inválido (Módulo 11 incorrecto) → 400 Bad Request")
    public void testRegistro_RunInvalido_Retorna400() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .run("11.111.111-9") // Dígito verificador incorrecto
                .nombres("Juan")
                .primerApellido("Pérez")
                .email("juan@eltabo.cl")
                .password("ClaveSegura#2026")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.run").exists());
    }

    @Test
    @DisplayName("POST /register con correo electrónico inválido → 400 Bad Request")
    public void testRegistro_EmailInvalido_Retorna400() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .run("11.111.111-1")
                .nombres("Juan")
                .primerApellido("Pérez")
                .email("correo-invalido-sin-arroba")
                .password("ClaveSegura#2026")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    @DisplayName("POST /login con email inválido → 400 Bad Request")
    public void testLogin_EmailInvalido_Retorna400() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("no-es-correo")
                .password("clave123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    @DisplayName("GET /incidentes/evidencias/** sin token → 403 Forbidden (Endpoint Protegido)")
    public void testEvidencias_SinAutenticacion_Retorna403() throws Exception {
        mockMvc.perform(get("/api/v1/incidentes/evidencias/foto123.jpg"))
                .andExpect(status().isForbidden());
    }
}
