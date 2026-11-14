package com.conectatech.sgs_backend.controller;

import com.conectatech.sgs_backend.dto.IncidenteResponseDTO;
import com.conectatech.sgs_backend.model.BitacoraProcedimiento;
import com.conectatech.sgs_backend.security.SecurityConfig;
import com.conectatech.sgs_backend.service.IncidenteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.conectatech.sgs_backend.security.JwtAuthenticationFilter;
import com.conectatech.sgs_backend.security.JwtUtil;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(IncidenteController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class }) // Carga explícita de las reglas de seguridad
public class IncidenteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Dependencia directa del controlador
    @MockitoBean
    private IncidenteService incidenteService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    // Tests GET /api/v1/incidentes

    @Test
    @DisplayName("GET /incidentes con rol OPERADOR → 200 OK + JSON")
    public void testObtenerIncidentes_ConRolOperador_Retorna200() throws Exception {
        IncidenteResponseDTO dto = new IncidenteResponseDTO();
        dto.setId("abc123");
        dto.setCategoria("SEGURIDAD");
        dto.setEstado("ABIERTO");
        dto.setFechaCreacion(LocalDateTime.now());

        when(incidenteService.obtenerTodos()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/incidentes")
                .with(user("test").roles("OPERADOR"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("abc123"))
                .andExpect(jsonPath("$[0].categoria").value("SEGURIDAD"));
    }

    @Test
    @DisplayName("GET /incidentes con rol ADMINISTRADOR → 200 OK")
    public void testObtenerIncidentes_ConRolAdmin_Retorna200() throws Exception {
        when(incidenteService.obtenerTodos()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/incidentes")
                .with(user("admin").roles("ADMINISTRADOR")))
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
    @DisplayName("GET /incidentes con rol CIUDADANO → 200 OK")
    public void testObtenerIncidentes_ConRolCiudadano_Retorna200() throws Exception {
        when(incidenteService.obtenerTodos()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/v1/incidentes")
                .with(user("ciudadano").roles("CIUDADANO")))
                .andExpect(status().isOk());
    }

    // Tests POST /api/v1/incidentes

    @Test
    @DisplayName("POST /incidentes sin datos con rol ADMINISTRADOR → 400 Bad Request")
    public void testCrearIncidente_SinDatos_Retorna400BadRequest() throws Exception {
        mockMvc.perform(multipart("/api/v1/incidentes")
                .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /incidentes con rol INSPECTOR → 403 Forbidden")
    public void testCrearIncidente_ConRolInspector_Retorna403() throws Exception {
        mockMvc.perform(multipart("/api/v1/incidentes")
                .with(user("inspector").roles("INSPECTOR")))
                .andExpect(status().isForbidden());
    }

    // Tests DELETE /api/v1/incidentes/{id}

    @Test
    @DisplayName("DELETE /incidentes/{id} con rol ADMINISTRADOR → 204 No Content")
    public void testEliminarIncidente_ConRolAdmin_Retorna204() throws Exception {
        doNothing().when(incidenteService).eliminarIncidente("incidente123");

        mockMvc.perform(delete("/api/v1/incidentes/incidente123")
                .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /incidentes/{id} con rol OPERADOR → 204 No Content")
    public void testEliminarIncidente_ConRolOperador_Retorna204() throws Exception {
        doNothing().when(incidenteService).eliminarIncidente("incidente123");

        mockMvc.perform(delete("/api/v1/incidentes/incidente123")
                .with(user("operador").roles("OPERADOR")))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /incidentes/{id} con rol INSPECTOR → 403 Forbidden")
    public void testEliminarIncidente_ConRolInspector_Retorna403() throws Exception {
        mockMvc.perform(delete("/api/v1/incidentes/incidente123")
                .with(user("inspector").roles("INSPECTOR")))
                .andExpect(status().isForbidden());
    }

    // Tests GET /api/v1/incidentes/bitacora/global

    @Test
    @DisplayName("GET /incidentes/bitacora/global con rol ADMINISTRADOR → 200 OK + Page JSON")
    public void testObtenerBitacoraGlobal_ConRolAdmin_Retorna200() throws Exception {
        BitacoraProcedimiento bitacora = BitacoraProcedimiento.builder()
                .id("bit123")
                .campoModificado("Estado")
                .valorAnterior("Pendiente")
                .valorNuevo("En proceso")
                .nombreActor("Admin Test")
                .rolActor("ADMINISTRADOR")
                .fechaModificacion(LocalDateTime.now())
                .build();

        when(incidenteService.obtenerHistorialGlobalPaginado(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(bitacora), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/incidentes/bitacora/global")
                .with(user("admin").roles("ADMINISTRADOR"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value("bit123"))
                .andExpect(jsonPath("$.content[0].campoModificado").value("Estado"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /incidentes/paginado con caracteres regex especiales → 200 OK")
    public void testObtenerIncidentesPaginado_ConCaracteresRegexEspeciales_Retorna200() throws Exception {
        IncidenteResponseDTO dto = new IncidenteResponseDTO();
        dto.setId("inc-paginado-1");
        dto.setCodigoCorrelativo("INC-999");
        dto.setDescripcion("Prueba con teléfono +569 1234 (urgente)");

        when(incidenteService.buscarIncidentesAvanzadoPaginado(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/incidentes/paginado")
                .param("textoBusqueda", "+569 (1234) [urgente]*")
                .with(user("operador").roles("OPERADOR"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("inc-paginado-1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("DELETE /incidentes/{id} cuando no existe → 404 Not Found")
    public void testEliminarIncidente_NoExiste_Retorna404() throws Exception {
        org.mockito.Mockito.doThrow(new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Incidente no encontrado con ID: no-existe"))
                .when(incidenteService).eliminarIncidente("no-existe");

        mockMvc.perform(delete("/api/v1/incidentes/no-existe")
                .with(user("admin").roles("ADMINISTRADOR"))
                .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Incidente no encontrado con ID: no-existe"));
    }

    @Test
    @DisplayName("DELETE /incidentes/{id} con motivo de justificación → 204 No Content")
    public void testEliminarIncidente_ConMotivo_Retorna204() throws Exception {
        org.mockito.Mockito.doNothing().when(incidenteService).eliminarIncidente("inc-123", "Denuncia duplicada");

        mockMvc.perform(delete("/api/v1/incidentes/inc-123")
                .param("motivo", "Denuncia duplicada")
                .with(user("admin").roles("ADMINISTRADOR"))
                .with(csrf()))
                .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(incidenteService).eliminarIncidente("inc-123", "Denuncia duplicada");
    }

    // Tests PATCH /api/v1/incidentes/{id}/reactivar

    @Test
    @DisplayName("PATCH /incidentes/{id}/reactivar con rol ADMINISTRADOR → 200 OK")
    public void testReactivarIncidente_ConRolAdmin_Retorna200() throws Exception {
        IncidenteResponseDTO dto = new IncidenteResponseDTO();
        dto.setId("inc123");
        dto.setCodigoCorrelativo("INC-100");
        dto.setActivo(true);

        when(incidenteService.reactivarIncidente(any(), any())).thenReturn(dto);

        mockMvc.perform(patch("/api/v1/incidentes/inc123/reactivar")
                .with(user("admin").roles("ADMINISTRADOR"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"Error de digitación\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("inc123"))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    @DisplayName("PATCH /incidentes/{id}/reactivar con rol OPERADOR → 403 Forbidden")
    public void testReactivarIncidente_ConRolOperador_Retorna403() throws Exception {
        mockMvc.perform(patch("/api/v1/incidentes/inc123/reactivar")
                .with(user("operador").roles("OPERADOR"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"Intento no autorizado\"}"))
                .andExpect(status().isForbidden());
    }
}
