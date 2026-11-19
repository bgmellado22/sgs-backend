package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.dto.IncidenteRequestDTO;
import com.conectatech.sgs_backend.dto.IncidenteResponseDTO;
import com.conectatech.sgs_backend.model.Incidente;
import com.conectatech.sgs_backend.model.Usuario;
import com.conectatech.sgs_backend.model.enums.RolUsuario;
import com.conectatech.sgs_backend.repository.BitacoraProcedimientoRepository;
import com.conectatech.sgs_backend.repository.IncidenteRepository;
import com.conectatech.sgs_backend.repository.ParametroSistemaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidenteServiceCreacionTest {

    @Mock
    private IncidenteRepository incidenteRepository;

    @Mock
    private SectorService sectorService;

    @Mock
    private NotificacionWebSocketService websocketService;

    @Mock
    private BitacoraProcedimientoRepository bitacoraRepository;

    @Mock
    private ParametroSistemaRepository parametroRepository;

    @Mock
    private GridFsTemplate gridFsTemplate;

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private IncidenteService incidenteService;

    private Usuario actorMock;

    @BeforeEach
    void setUp() {
        actorMock = new Usuario();
        actorMock.setId("usr-001");
        actorMock.setNombres("Juan");
        actorMock.setPrimerApellido("Pérez");
        actorMock.setRol(RolUsuario.OPERADOR);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(actorMock);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Al registrar una nueva denuncia estándar, NO se debe despachar notificación por WebSocket")
    void crearIncidente_Estandar_NoDespachaNotificacion() {
        IncidenteRequestDTO dto = new IncidenteRequestDTO();
        dto.setCategoria("SEGURIDAD");
        dto.setTipo("Ruidos Molestos");
        dto.setDescripcion("Música a alto volumen en la vía pública");
        dto.setLatitud(-33.456);
        dto.setLongitud(-71.654);
        dto.setDireccionTexto("Av. Playa Blanca 123");

        when(incidenteRepository.save(any(Incidente.class))).thenAnswer(invocation -> {
            Incidente inc = invocation.getArgument(0);
            inc.setId("inc-999");
            return inc;
        });

        IncidenteResponseDTO response = incidenteService.crearIncidente(dto);

        assertNotNull(response);
        // Regla obligatoria: No debe notificar en Notificaciones al registrar nueva denuncia
        verify(websocketService, never()).despacharAlertaGlobal(any());
        verify(websocketService, never()).despacharNotificacionPrivada(any());
    }

    @Test
    @DisplayName("Al registrar una alerta SOS, SÍ debe despachar la alerta global SOS")
    void crearIncidente_SOS_DespachaAlertaSOS() {
        IncidenteRequestDTO dto = new IncidenteRequestDTO();
        dto.setCategoria("SOS");
        dto.setTipo("BOTÓN DE PÁNICO");
        dto.setDescripcion("Emergencia SOS");
        dto.setLatitud(-33.456);
        dto.setLongitud(-71.654);
        dto.setDireccionTexto("Av. Central 456");

        when(incidenteRepository.save(any(Incidente.class))).thenAnswer(invocation -> {
            Incidente inc = invocation.getArgument(0);
            inc.setId("inc-sos-1");
            return inc;
        });

        IncidenteResponseDTO response = incidenteService.crearIncidente(dto);

        assertNotNull(response);
        verify(websocketService, times(1)).despacharAlertaGlobal(any());
    }
}
