package com.conectatech.sgs_backend.controller;

import com.conectatech.sgs_backend.dto.IncidenteResponseDTO;
import com.conectatech.sgs_backend.security.SecurityConfig;
import com.conectatech.sgs_backend.service.IncidenteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.conectatech.sgs_backend.security.JwtUtil;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IncidenteController.class)
@Import(SecurityConfig.class) // Carga explícita de las reglas de seguridad
public class IncidenteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Dependencia directa del controlador ──
    @MockitoBean
    private IncidenteService incidenteService;

    // ── Dependencias de la cadena de seguridad ──
    // SecurityConfig necesita: JwtAuthenticationFilter (cargado como Filter) +
    // AuthenticationProvider
    // JwtAuthenticationFilter necesita: JwtUtil + UserDetailsService
    // Al mockearlas, el filtro real se instancia pero al no recibir JWT simplemente
    // pasa de largo,
    // dejando que @WithMockUser establezca el SecurityContext correctamente.
    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    // ────────────── Tests GET /api/v1/incidentes ──────────────

    @Test
    @DisplayName("GET /incidentes con rol OPERADOR → 200 OK + JSON")
    @WithMockUser(roles = "OPERADOR")
    public void testObtenerIncidentes_ConRolOperador_Retorna200() throws Exception {
        IncidenteResponseDTO dto = new IncidenteResponseDTO();
        dto.setId("abc123");
        dto.setCategoria("SEGURIDAD");
        dto.setEstado("ABIERTO");
        dto.setFechaCreacion(LocalDateTime.now());

        when(incidenteService.obtenerTodos()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/incidentes")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("abc123"))
                .andExpect(jsonPath("$[0].categoria").value("SEGURIDAD"));
    }

    @Test
    @DisplayName("GET /incidentes con rol ADMINISTRADOR → 200 OK")
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testObtenerIncidentes_ConRolAdmin_Retorna200() throws Exception {
        when(incidenteService.obtenerTodos()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/incidentes"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("GET /incidentes sin autenticación → 403 Forbidden")
    public void testObtenerIncidentes_SinAuth_Retorna403() throws Exception {
        mockMvc.perform(get("/api/v1/incidentes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /incidentes con rol CIUDADANO → 403 Forbidden")
    @WithMockUser(roles = "CIUDADANO")
    public void testObtenerIncidentes_ConRolCiudadano_Retorna403() throws Exception {
        mockMvc.perform(get("/api/v1/incidentes"))
                .andExpect(status().isForbidden());
    }

    // ────────────── Tests POST /api/v1/incidentes ──────────────

    @Test
    @DisplayName("POST /incidentes sin datos con rol ADMINISTRADOR → 400 Bad Request")
    @WithMockUser(roles = "ADMINISTRADOR")
    public void testCrearIncidente_SinDatos_Retorna400BadRequest() throws Exception {
        mockMvc.perform(multipart("/api/v1/incidentes"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /incidentes con rol SUPERVISOR → 403 Forbidden")
    @WithMockUser(roles = "SUPERVISOR")
    public void testCrearIncidente_ConRolSupervisor_Retorna403() throws Exception {
        mockMvc.perform(multipart("/api/v1/incidentes"))
                .andExpect(status().isForbidden());
    }
}