package com.conectatech.sgs_backend.security;

import com.conectatech.sgs_backend.controller.UsuarioController;
import com.conectatech.sgs_backend.dto.MiPerfilUpdateDTO;
import com.conectatech.sgs_backend.dto.UsuarioResponseDTO;
import com.conectatech.sgs_backend.model.Usuario;
import com.conectatech.sgs_backend.model.enums.RolUsuario;
import com.conectatech.sgs_backend.service.UsuarioService;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
public class UsuarioSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("PUT /api/v1/usuarios/me con rol OPERADOR → 200 OK (Perfil propio no bloqueado por /usuarios/**)")
    public void testActualizarMiPerfil_ConRolOperador_Retorna200() throws Exception {
        MiPerfilUpdateDTO updateDTO = new MiPerfilUpdateDTO();
        updateDTO.setNombres("Carlos Modificado");
        updateDTO.setPrimerApellido("López");
        updateDTO.setEmail("carlos.modificado@eltabo.cl");

        UsuarioResponseDTO responseDTO = UsuarioResponseDTO.builder()
                .id("user123")
                .nombres("Carlos Modificado")
                .primerApellido("López")
                .email("carlos.modificado@eltabo.cl")
                .rol(RolUsuario.OPERADOR)
                .estado(true)
                .build();

        when(usuarioService.actualizarMiPerfil(any(MiPerfilUpdateDTO.class))).thenReturn(responseDTO);

        Usuario mockPrincipal = Usuario.builder()
                .id("user123")
                .email("carlos@eltabo.cl")
                .rol(RolUsuario.OPERADOR)
                .build();

        mockMvc.perform(put("/api/v1/usuarios/me")
                .with(user(mockPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombres").value("Carlos Modificado"))
                .andExpect(jsonPath("$.email").value("carlos.modificado@eltabo.cl"));
    }

    @Test
    @DisplayName("GET /api/v1/usuarios con rol ADMINISTRADOR → 200 OK")
    public void testListarUsuarios_ConRolAdmin_Retorna200() throws Exception {
        when(usuarioService.obtenerTodosLosUsuarios()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/usuarios")
                .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/usuarios con rol OPERADOR → 403 Forbidden")
    public void testListarUsuarios_ConRolOperador_Retorna403() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios")
                .with(user("operador").roles("OPERADOR")))
                .andExpect(status().isForbidden());
    }
}
