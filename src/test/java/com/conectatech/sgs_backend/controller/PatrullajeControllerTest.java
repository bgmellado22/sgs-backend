package com.conectatech.sgs_backend.controller;

import com.conectatech.sgs_backend.dto.CierrePatrullajeDTO;
import com.conectatech.sgs_backend.dto.CrearPatrullajeDTO;
import com.conectatech.sgs_backend.dto.PatrullajeDTO;
import com.conectatech.sgs_backend.dto.TelemetriaInspectorDTO;
import com.conectatech.sgs_backend.model.enums.EstadoPatrullaje;
import com.conectatech.sgs_backend.model.enums.ResultadoProcedimiento;
import com.conectatech.sgs_backend.security.JwtAuthenticationFilter;
import com.conectatech.sgs_backend.security.JwtUtil;
import com.conectatech.sgs_backend.security.SecurityConfig;
import com.conectatech.sgs_backend.service.PatrullajeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PatrullajeController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
public class PatrullajeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private PatrullajeService patrullajeService;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/v1/patrullajes - Rol OPERADOR puede crear patrullaje preventivo (201 Created)")
    void crearPatrullaje_RolOperador_Retorna201() throws Exception {
        CrearPatrullajeDTO dto = CrearPatrullajeDTO.builder()
                .latitud(-33.4532)
                .longitud(-71.6654)
                .sector("El Tabo Centro")
                .inspectorId("insp-1")
                .tipoPatrullaje("VEHICULAR_DISUASIVO")
                .objetivo("Ronda disuasiva en centros comerciales")
                .tiempoEstimadoMinutos(45)
                .build();

        PatrullajeDTO respuestaDTO = PatrullajeDTO.builder()
                .id("pat-123")
                .codigoCorrelativo("PAT-2026-0001")
                .estado(EstadoPatrullaje.EN_PROCESO)
                .sector("El Tabo Centro")
                .tiempoEstimadoMinutos(45)
                .build();

        when(patrullajeService.crearPatrullaje(any(CrearPatrullajeDTO.class))).thenReturn(respuestaDTO);

        mockMvc.perform(post("/api/v1/patrullajes")
                        .with(user("operador1").roles("OPERADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("pat-123"))
                .andExpect(jsonPath("$.codigoCorrelativo").value("PAT-2026-0001"))
                .andExpect(jsonPath("$.estado").value("EN_PROCESO"));
    }

    @Test
    @DisplayName("POST /api/v1/patrullajes - Rol CIUDADANO es denegado (403 Forbidden)")
    void crearPatrullaje_RolCiudadano_Retorna403() throws Exception {
        CrearPatrullajeDTO dto = CrearPatrullajeDTO.builder()
                .latitud(-33.4532)
                .longitud(-71.6654)
                .sector("El Tabo Centro")
                .inspectorId("insp-1")
                .tipoPatrullaje("VEHICULAR_DISUASIVO")
                .objetivo("Ronda disuasiva")
                .tiempoEstimadoMinutos(45)
                .build();

        mockMvc.perform(post("/api/v1/patrullajes")
                        .with(user("ciudadano1").roles("CIUDADANO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/patrullajes/activos - Rol INSPECTOR puede listar servicios en terreno (200 OK)")
    void listarActivos_RolInspector_Retorna200() throws Exception {
        PatrullajeDTO pat = PatrullajeDTO.builder()
                .id("pat-1")
                .codigoCorrelativo("PAT-100")
                .estado(EstadoPatrullaje.EN_PROCESO)
                .build();

        when(patrullajeService.listarActivos()).thenReturn(List.of(pat));

        mockMvc.perform(get("/api/v1/patrullajes/activos")
                        .with(user("inspector1").roles("INSPECTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigoCorrelativo").value("PAT-100"));
    }

    @Test
    @DisplayName("PATCH /api/v1/patrullajes/{id}/en-sitio - Inspector marca arribo a terreno (200 OK)")
    void marcarEnSitio_RolInspector_Retorna200() throws Exception {
        PatrullajeDTO respuesta = PatrullajeDTO.builder()
                .id("pat-1")
                .estado(EstadoPatrullaje.EN_SITIO)
                .build();

        when(patrullajeService.marcarEnSitio("pat-1")).thenReturn(respuesta);

        mockMvc.perform(patch("/api/v1/patrullajes/pat-1/en-sitio")
                        .with(user("inspector1").roles("INSPECTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_SITIO"));
    }

    @Test
    @DisplayName("POST /api/v1/patrullajes/{id}/cerrar - Cierre formal multipart (200 OK)")
    void cerrarPatrullaje_Multipart_Retorna200() throws Exception {
        CierrePatrullajeDTO datosDTO = CierrePatrullajeDTO.builder()
                .resultadoProcedimiento(ResultadoProcedimiento.SIN_NOVEDAD)
                .observaciones("Vigilancia cumplida con éxito")
                .build();

        MockMultipartFile datosPart = new MockMultipartFile(
                "datos",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(datosDTO)
        );

        MockMultipartFile fotoPart = new MockMultipartFile(
                "evidencia",
                "foto.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[]{1, 2, 3}
        );

        PatrullajeDTO cerrado = PatrullajeDTO.builder()
                .id("pat-1")
                .estado(EstadoPatrullaje.CERRADO)
                .resultadoProcedimiento(ResultadoProcedimiento.SIN_NOVEDAD)
                .build();

        when(patrullajeService.cerrarPatrullaje(eq("pat-1"), any(CierrePatrullajeDTO.class), any()))
                .thenReturn(cerrado);

        mockMvc.perform(multipart("/api/v1/patrullajes/pat-1/cerrar")
                        .file(datosPart)
                        .file(fotoPart)
                        .with(user("inspector1").roles("INSPECTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADO"))
                .andExpect(jsonPath("$.resultadoProcedimiento").value("SIN_NOVEDAD"));
    }

    @Test
    @DisplayName("POST /api/v1/patrullajes/telemetria - Inspector envía coordenadas cada 10 segundos (202 Accepted)")
    void enviarTelemetria_Retorna202() throws Exception {
        TelemetriaInspectorDTO dto = TelemetriaInspectorDTO.builder()
                .inspectorId("insp-1")
                .latitud(-33.4532)
                .longitud(-71.6654)
                .precisionMetros(4.5)
                .build();

        mockMvc.perform(post("/api/v1/patrullajes/telemetria")
                        .with(user("inspector1").roles("INSPECTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isAccepted());

        verify(messagingTemplate).convertAndSend(eq("/topic/telemetria-inspectores"), any(TelemetriaInspectorDTO.class));
    }
}
