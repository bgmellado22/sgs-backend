package com.conectatech.sgs_backend.controller;

import com.conectatech.sgs_backend.model.Notificacion;
import com.conectatech.sgs_backend.model.Usuario;
import com.conectatech.sgs_backend.model.enums.EstadoNotificacion;
import com.conectatech.sgs_backend.model.enums.RolUsuario;
import com.conectatech.sgs_backend.model.enums.TipoNotificacion;
import com.conectatech.sgs_backend.security.JwtAuthenticationFilter;
import com.conectatech.sgs_backend.security.JwtUtil;
import com.conectatech.sgs_backend.security.SecurityConfig;
import com.conectatech.sgs_backend.service.NotificacionService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificacionController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
public class NotificacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificacionService notificacionService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /api/v1/notificaciones/paginado retorna 200 OK con Page<Notificacion> para scroll infinito")
    public void testObtenerNotificacionesPaginado_Retorna200YPage() throws Exception {
        Usuario usuarioMock = Usuario.builder()
                .id("usr-123")
                .email("operador@eltabo.cl")
                .rol(RolUsuario.OPERADOR)
                .build();

        Notificacion notif1 = Notificacion.builder()
                .id("notif-1")
                .titulo("Incidente Asignado")
                .mensaje("Se ha asignado patrulla a Las Cruces")
                .tipo(TipoNotificacion.INFORMATIVO)
                .estado(EstadoNotificacion.NO_LEIDA)
                .fechaCreacion(LocalDateTime.now())
                .usuarioDestinoId("usr-123")
                .build();

        when(notificacionService.obtenerNotificacionesPaginadasPorUsuario(eq("usr-123"), any()))
                .thenReturn(new PageImpl<>(List.of(notif1), PageRequest.of(0, 15), 1));

        mockMvc.perform(get("/api/v1/notificaciones/paginado")
                .param("page", "0")
                .param("size", "15")
                .with(user(usuarioMock))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value("notif-1"))
                .andExpect(jsonPath("$.content[0].titulo").value("Incidente Asignado"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
