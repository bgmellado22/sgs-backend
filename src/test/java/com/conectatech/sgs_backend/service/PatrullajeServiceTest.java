package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.dto.CierrePatrullajeDTO;
import com.conectatech.sgs_backend.dto.CrearPatrullajeDTO;
import com.conectatech.sgs_backend.dto.PatrullajeDTO;
import com.conectatech.sgs_backend.model.Patrullaje;
import com.conectatech.sgs_backend.model.Usuario;
import com.conectatech.sgs_backend.model.enums.EstadoPatrullaje;
import com.conectatech.sgs_backend.model.enums.ResultadoProcedimiento;
import com.conectatech.sgs_backend.repository.PatrullajeRepository;
import com.conectatech.sgs_backend.repository.UsuarioRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PatrullajeServiceTest {

    @Mock
    private PatrullajeRepository patrullajeRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private GridFsTemplate gridFsTemplate;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private FcmService fcmService;

    @InjectMocks
    private PatrullajeService patrullajeService;

    private Usuario inspectorMock;

    @BeforeEach
    void setUp() {
        inspectorMock = Usuario.builder()
                .id("insp-123")
                .nombres("Carlos")
                .primerApellido("Mendoza")
                .nombreCompleto("Carlos Mendoza")
                .fcmToken("fcm-token-test-12345")
                .build();
    }

    @Test
    @DisplayName("Debe crear patrullaje preventivo exitosamente, auditar, notificar WS y enviar Push FCM")
    void crearPatrullaje_Exitoso() {
        CrearPatrullajeDTO dto = CrearPatrullajeDTO.builder()
                .latitud(-33.4532)
                .longitud(-71.6654)
                .coordenadasTexto("-33.4532, -71.6654")
                .sector("El Tabo Centro")
                .inspectorId("insp-123")
                .tipoPatrullaje("VEHICULAR_DISUASIVO")
                .objetivo("Ronda preventiva en sector comercial")
                .tiempoEstimadoMinutos(45)
                .build();

        when(usuarioRepository.findById("insp-123")).thenReturn(Optional.of(inspectorMock));
        when(patrullajeRepository.save(any(Patrullaje.class))).thenAnswer(invocation -> {
            Patrullaje p = invocation.getArgument(0);
            p.setId("pat-generated-id");
            return p;
        });

        PatrullajeDTO resultado = patrullajeService.crearPatrullaje(dto);

        assertNotNull(resultado);
        assertEquals("pat-generated-id", resultado.getId());
        assertTrue(resultado.getCodigoCorrelativo().startsWith("PAT-"));
        assertEquals(EstadoPatrullaje.EN_PROCESO, resultado.getEstado());
        assertEquals("Carlos Mendoza", resultado.getInspectorNombre());
        assertEquals(45, resultado.getTiempoEstimadoMinutos());

        verify(auditoriaService).registrarAuditoria(eq("pat-generated-id"), anyString(), eq("ESTADO"), isNull(), eq("En Proceso"), anyString());
        verify(messagingTemplate).convertAndSend(eq("/topic/patrullajes"), any(Object.class));
        verify(fcmService).enviarNotificacionPush(eq("fcm-token-test-12345"), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("Debe marcar 'En Sitio' y registrar hito temporal y de auditoría")
    void marcarEnSitio_Exitoso() {
        Patrullaje patrullajeExistente = Patrullaje.builder()
                .id("pat-1")
                .codigoCorrelativo("PAT-1001")
                .sector("Playa Chica")
                .estado(EstadoPatrullaje.EN_PROCESO)
                .build();

        when(patrullajeRepository.findById("pat-1")).thenReturn(Optional.of(patrullajeExistente));
        when(patrullajeRepository.save(any(Patrullaje.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatrullajeDTO resultado = patrullajeService.marcarEnSitio("pat-1");

        assertNotNull(resultado);
        assertEquals(EstadoPatrullaje.EN_SITIO, resultado.getEstado());
        assertNotNull(resultado.getFechaLlegadaSitio());

        verify(auditoriaService).registrarAuditoria(eq("pat-1"), eq("PAT-1001"), eq("ESTADO"), eq("En Proceso"), eq("En Sitio"), anyString());
        verify(messagingTemplate).convertAndSend(eq("/topic/patrullajes"), any(Object.class));
    }

    @Test
    @DisplayName("Debe lanzar BAD_REQUEST si se intenta marcar 'En Sitio' un patrullaje ya cerrado")
    void marcarEnSitio_Falla_SiYaCerrado() {
        Patrullaje cerrado = Patrullaje.builder()
                .id("pat-cerrado")
                .estado(EstadoPatrullaje.CERRADO)
                .build();

        when(patrullajeRepository.findById("pat-cerrado")).thenReturn(Optional.of(cerrado));

        assertThrows(ResponseStatusException.class, () -> patrullajeService.marcarEnSitio("pat-cerrado"));
    }

    @Test
    @DisplayName("Debe cerrar patrullaje con fotografía en GridFS y programar depuración a 90 días para 'Sin Novedad'")
    void cerrarPatrullaje_ExitosoConFoto_SinNovedad() {
        Patrullaje enSitio = Patrullaje.builder()
                .id("pat-sitio")
                .codigoCorrelativo("PAT-2002")
                .estado(EstadoPatrullaje.EN_SITIO)
                .fechaCreacion(LocalDateTime.now().minusMinutes(40))
                .build();

        CierrePatrullajeDTO cierreDTO = CierrePatrullajeDTO.builder()
                .resultadoProcedimiento(ResultadoProcedimiento.SIN_NOVEDAD)
                .observaciones("Ronda sin novedades en el cuadrante costero")
                .build();

        MockMultipartFile foto = new MockMultipartFile(
                "evidencia",
                "evidencia_patrulla.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3, 4, 5}
        );

        ObjectId gridFsIdMock = new ObjectId();
        when(patrullajeRepository.findById("pat-sitio")).thenReturn(Optional.of(enSitio));
        when(gridFsTemplate.store(any(InputStream.class), eq("evidencia_patrulla.jpg"), eq("image/jpeg")))
                .thenReturn(gridFsIdMock);
        when(patrullajeRepository.save(any(Patrullaje.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatrullajeDTO resultado = patrullajeService.cerrarPatrullaje("pat-sitio", cierreDTO, foto);

        assertNotNull(resultado);
        assertEquals(EstadoPatrullaje.CERRADO, resultado.getEstado());
        assertEquals(ResultadoProcedimiento.SIN_NOVEDAD, resultado.getResultadoProcedimiento());
        assertEquals(gridFsIdMock.toHexString(), resultado.getEvidenciaFotoId());
        assertTrue(resultado.getDuracionEfectivaMinutos() >= 39);

        // Verifica que se haya programado la fecha de depuración
        verify(patrullajeRepository).save(argThat(p -> p.getFechaDepuracionEvidencia() != null));
        verify(auditoriaService).registrarAuditoria(eq("pat-sitio"), eq("PAT-2002"), eq("ESTADO"), eq("En Sitio"), eq("Cerrado"), anyString());
        verify(messagingTemplate).convertAndSend(eq("/topic/patrullajes"), any(Object.class));
    }

    @Test
    @DisplayName("Debe lanzar BAD_REQUEST si la foto adjunta excede los 10MB")
    void cerrarPatrullaje_Falla_FotoExcede10MB() {
        Patrullaje enSitio = Patrullaje.builder()
                .id("pat-sitio")
                .estado(EstadoPatrullaje.EN_SITIO)
                .build();

        CierrePatrullajeDTO cierreDTO = CierrePatrullajeDTO.builder()
                .resultadoProcedimiento(ResultadoProcedimiento.SIN_NOVEDAD)
                .build();

        // 11MB
        byte[] contenidoPesado = new byte[11 * 1024 * 1024];
        MockMultipartFile fotoPesada = new MockMultipartFile("evidencia", "foto.jpg", "image/jpeg", contenidoPesado);

        when(patrullajeRepository.findById("pat-sitio")).thenReturn(Optional.of(enSitio));

        assertThrows(ResponseStatusException.class, () ->
                patrullajeService.cerrarPatrullaje("pat-sitio", cierreDTO, fotoPesada)
        );
    }

    @Test
    @DisplayName("Debe depurar de GridFS las fotos que cumplieron los 90 días")
    void ejecutarDepuracionEvidenciasSinNovedad_EliminaArchivosExpirados() {
        ObjectId fotoId = new ObjectId();
        Patrullaje patrullajeExpirado = Patrullaje.builder()
                .id("pat-expirado")
                .codigoCorrelativo("PAT-OLD")
                .resultadoProcedimiento(ResultadoProcedimiento.SIN_NOVEDAD)
                .evidenciaFotoId(fotoId.toHexString())
                .evidenciaDepurada(false)
                .fechaDepuracionEvidencia(LocalDateTime.now().minusDays(1))
                .build();

        when(patrullajeRepository.findByResultadoProcedimientoAndEvidenciaFotoIdNotNullAndEvidenciaDepuradaFalseAndFechaDepuracionEvidenciaBefore(
                eq(ResultadoProcedimiento.SIN_NOVEDAD), any(LocalDateTime.class)
        )).thenReturn(List.of(patrullajeExpirado));

        patrullajeService.ejecutarDepuracionEvidenciasSinNovedad();

        verify(gridFsTemplate).delete(any(Query.class));
        verify(patrullajeRepository).save(argThat(p -> Boolean.TRUE.equals(p.getEvidenciaDepurada())));
    }

    @Test
    @DisplayName("Debe cancelar un patrullaje activo exitosamente, auditar, notificar WS y enviar Push")
    void cancelarPatrullaje_Exitoso() {
        Patrullaje activo = Patrullaje.builder()
                .id("pat-canc-1")
                .codigoCorrelativo("PAT-999")
                .estado(EstadoPatrullaje.EN_PROCESO)
                .inspectorId("insp-123")
                .sector("Playa Chica")
                .build();

        when(patrullajeRepository.findById("pat-canc-1")).thenReturn(Optional.of(activo));
        when(patrullajeRepository.save(any(Patrullaje.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.findById("insp-123")).thenReturn(Optional.of(inspectorMock));

        PatrullajeDTO resultado = patrullajeService.cancelarPatrullaje("pat-canc-1", "Cambio de cuadrante prioritario");

        assertNotNull(resultado);
        assertEquals(EstadoPatrullaje.CANCELADO, resultado.getEstado());
        assertEquals("Cambio de cuadrante prioritario", resultado.getObservacionesCierre());
        verify(auditoriaService).registrarAuditoria(
                eq("pat-canc-1"), eq("PAT-999"), eq("ESTADO"), eq("En Proceso"), eq("Cancelado"), contains("Cambio de cuadrante")
        );
        verify(messagingTemplate).convertAndSend(eq("/topic/patrullajes"), any(Object.class));
        verify(fcmService).enviarNotificacionPush(eq("fcm-token-test-12345"), contains("Cancelado"), anyString(), anyMap());
    }

    @Test
    @DisplayName("Debe lanzar excepción al intentar cancelar un patrullaje ya cerrado")
    void cancelarPatrullaje_YaCerrado_LanzaExcepcion() {
        Patrullaje cerrado = Patrullaje.builder()
                .id("pat-cerrado")
                .estado(EstadoPatrullaje.CERRADO)
                .build();

        when(patrullajeRepository.findById("pat-cerrado")).thenReturn(Optional.of(cerrado));

        assertThrows(ResponseStatusException.class, () ->
                patrullajeService.cancelarPatrullaje("pat-cerrado", "motivo")
        );
    }
}

