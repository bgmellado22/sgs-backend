package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.dto.*;
import com.conectatech.sgs_backend.model.Patrullaje;
import com.conectatech.sgs_backend.model.Usuario;
import com.conectatech.sgs_backend.model.enums.EstadoPatrullaje;
import com.conectatech.sgs_backend.model.enums.ResultadoProcedimiento;
import com.conectatech.sgs_backend.repository.PatrullajeRepository;
import com.conectatech.sgs_backend.repository.UsuarioRepository;
import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatrullajeService {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L; // 10MB (AGENTS.md)

    private final PatrullajeRepository patrullajeRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;
    private final GridFsTemplate gridFsTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final FcmService fcmService;

    /**
     * Registra un nuevo patrullaje preventivo despachado desde la central táctica.
     */
    public PatrullajeDTO crearPatrullaje(CrearPatrullajeDTO dto) {
        long correlativoUnico = (System.currentTimeMillis() % 900000L) + 100000L;
        String codigoCorrelativo = "PAT-" + correlativoUnico;

        // Validar y obtener inspector asignado
        String nombreInspector = dto.getInspectorNombre();
        String fcmToken = null;
        if (dto.getInspectorId() != null) {
            Usuario inspector = usuarioRepository.findById(dto.getInspectorId()).orElse(null);
            if (inspector != null) {
                nombreInspector = inspector.getNombreCompleto();
                fcmToken = inspector.getFcmToken();
            }
        }

        Patrullaje patrullaje = Patrullaje.builder()
                .codigoCorrelativo(codigoCorrelativo)
                .location(new GeoJsonPoint(dto.getLongitud(), dto.getLatitud()))
                .latitud(dto.getLatitud())
                .longitud(dto.getLongitud())
                .coordenadasTexto(dto.getCoordenadasTexto())
                .sector(dto.getSector())
                .inspectorId(dto.getInspectorId())
                .inspectorNombre(nombreInspector)
                .tipoPatrullaje(dto.getTipoPatrullaje())
                .objetivo(dto.getObjetivo())
                .instruccionesTacticas(dto.getInstruccionesTacticas())
                .tiempoEstimadoMinutos(dto.getTiempoEstimadoMinutos())
                .estado(EstadoPatrullaje.EN_PROCESO)
                .fechaCreacion(LocalDateTime.now())
                .fechaAsignacion(LocalDateTime.now())
                .activo(true)
                .evidenciaDepurada(false)
                .build();

        Patrullaje guardado = patrullajeRepository.save(patrullaje);

        // Registro de auditoría forense
        auditoriaService.registrarAuditoria(
                guardado.getId(),
                guardado.getCodigoCorrelativo(),
                "ESTADO",
                null,
                EstadoPatrullaje.EN_PROCESO.name(),
                "Patrullaje preventivo despachado a " + guardado.getInspectorNombre() + " en " + guardado.getSector()
        );

        PatrullajeDTO responseDto = mapToDTO(guardado);

        // Notificación en tiempo real por WebSocket
        messagingTemplate.convertAndSend("/topic/patrullajes", new EventoPatrullajeWS("CREADO", responseDto, LocalDateTime.now()));

        // Notificación Push de alta prioridad hacia el inspector asignado
        if (fcmToken != null && !fcmToken.isBlank()) {
            Map<String, String> data = new HashMap<>();
            data.put("tipo", "DESPACHO_PATRULLAJE");
            data.put("patrullajeId", guardado.getId());
            data.put("codigoCorrelativo", guardado.getCodigoCorrelativo());
            data.put("sector", guardado.getSector());
            data.put("objetivo", guardado.getObjetivo());
            data.put("latitud", String.valueOf(guardado.getLatitud()));
            data.put("longitud", String.valueOf(guardado.getLongitud()));
            data.put("tiempoEstimadoMinutos", String.valueOf(guardado.getTiempoEstimadoMinutos()));

            fcmService.enviarNotificacionPush(
                    fcmToken,
                    "🚨 Nuevo Patrullaje: " + guardado.getSector(),
                    guardado.getObjetivo(),
                    data
            );
        }

        return responseDto;
    }

    /**
     * Lista todos los patrullajes preventivos actualmente en ronda activa (EN_PROCESO o EN_SITIO).
     */
    public List<PatrullajeDTO> listarActivos() {
        return patrullajeRepository.findByActivoTrueAndEstadoInOrderByFechaCreacionDesc(
                List.of(EstadoPatrullaje.EN_PROCESO, EstadoPatrullaje.EN_SITIO)
        ).stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    /**
     * Obtiene el detalle de un patrullaje por ID.
     */
    public PatrullajeDTO obtenerPorId(String id) {
        Patrullaje patrullaje = patrullajeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patrullaje no encontrado con ID: " + id));
        return mapToDTO(patrullaje);
    }

    /**
     * Registra el arribo a terreno ('EN_SITIO') por parte del inspector.
     */
    public PatrullajeDTO marcarEnSitio(String id) {
        Patrullaje patrullaje = patrullajeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patrullaje no encontrado con ID: " + id));

        if (patrullaje.getEstado() == EstadoPatrullaje.CERRADO || patrullaje.getEstado() == EstadoPatrullaje.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede marcar en sitio un servicio que ya está " + patrullaje.getEstado());
        }

        EstadoPatrullaje estadoAnterior = patrullaje.getEstado();
        patrullaje.setEstado(EstadoPatrullaje.EN_SITIO);
        patrullaje.setFechaLlegadaSitio(LocalDateTime.now());

        Patrullaje actualizado = patrullajeRepository.save(patrullaje);

        // Registro de auditoría
        auditoriaService.registrarAuditoria(
                actualizado.getId(),
                actualizado.getCodigoCorrelativo(),
                "ESTADO",
                estadoAnterior.name(),
                EstadoPatrullaje.EN_SITIO.name(),
                "Inspector arribó al sector de cobertura: " + actualizado.getSector()
        );

        PatrullajeDTO responseDto = mapToDTO(actualizado);

        // Notificar cambio por WebSocket
        messagingTemplate.convertAndSend("/topic/patrullajes", new EventoPatrullajeWS("EN_SITIO", responseDto, LocalDateTime.now()));

        return responseDto;
    }

    /**
     * Cierre formal del servicio de patrullaje con resultado, observaciones y foto opcional en GridFS.
     */
    public PatrullajeDTO cerrarPatrullaje(String id, CierrePatrullajeDTO datos, MultipartFile foto) {
        Patrullaje patrullaje = patrullajeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patrullaje no encontrado con ID: " + id));

        if (patrullaje.getEstado() == EstadoPatrullaje.CERRADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El patrullaje ya se encuentra cerrado.");
        }

        EstadoPatrullaje estadoAnterior = patrullaje.getEstado();
        patrullaje.setEstado(EstadoPatrullaje.CERRADO);
        patrullaje.setFechaCierre(LocalDateTime.now());
        patrullaje.setResultadoProcedimiento(datos.getResultadoProcedimiento());
        patrullaje.setObservacionesCierre(datos.getObservaciones());

        // Calcular duración efectiva
        if (patrullaje.getFechaCreacion() != null) {
            long minutos = Duration.between(patrullaje.getFechaCreacion(), patrullaje.getFechaCierre()).toMinutes();
            patrullaje.setDuracionEfectivaMinutos(Math.max(1, minutos));
        }

        // Subida de fotografía opcional a GridFS
        if (foto != null && !foto.isEmpty()) {
            if (foto.getSize() > MAX_FILE_SIZE_BYTES) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fotografía excede el tamaño máximo permitido de 10MB.");
            }
            String contentType = foto.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se permite adjuntar imágenes (JPG, PNG, WEBP).");
            }
            try {
                ObjectId fileId = gridFsTemplate.store(
                        foto.getInputStream(),
                        foto.getOriginalFilename(),
                        contentType
                );
                patrullaje.setEvidenciaFotoId(fileId.toHexString());
            } catch (IOException e) {
                log.error("Error al almacenar la evidencia de patrullaje en GridFS", e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al almacenar la evidencia fotográfica.");
            }
        }

        // Si el resultado fue 'SIN_NOVEDAD', se programa la depuración a 90 días
        if (datos.getResultadoProcedimiento() == ResultadoProcedimiento.SIN_NOVEDAD && patrullaje.getEvidenciaFotoId() != null) {
            patrullaje.setFechaDepuracionEvidencia(LocalDateTime.now().plusDays(90));
        }

        Patrullaje cerrado = patrullajeRepository.save(patrullaje);

        // Registro de auditoría
        auditoriaService.registrarAuditoria(
                cerrado.getId(),
                cerrado.getCodigoCorrelativo(),
                "ESTADO",
                estadoAnterior.name(),
                EstadoPatrullaje.CERRADO.name(),
                "Patrullaje preventivo finalizado. Resultado: " + cerrado.getResultadoProcedimiento() +
                        (cerrado.getObservacionesCierre() != null ? " - " + cerrado.getObservacionesCierre() : "")
        );

        PatrullajeDTO responseDto = mapToDTO(cerrado);

        // Notificar desmontaje visual a la Web
        messagingTemplate.convertAndSend("/topic/patrullajes", new EventoPatrullajeWS("CERRADO", responseDto, LocalDateTime.now()));

        return responseDto;
    }

    /**
     * Descarga de recurso fotográfico desde GridFS.
     */
    public Resource descargarEvidencia(String id) {
        Patrullaje patrullaje = patrullajeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patrullaje no encontrado"));

        String gridFsId = patrullaje.getEvidenciaFotoId();
        if (gridFsId == null || gridFsId.isBlank() || Boolean.TRUE.equals(patrullaje.getEvidenciaDepurada())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El patrullaje no cuenta con evidencia fotográfica activa o ya fue depurada.");
        }

        GridFSFile file;
        if (ObjectId.isValid(gridFsId)) {
            file = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(new ObjectId(gridFsId))));
        } else {
            file = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(gridFsId)));
        }

        if (file == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo de evidencia no encontrado en GridFS");
        }

        return gridFsTemplate.getResource(file);
    }

    /**
     * Tarea programada diaria para depuración de fotografías de 'Sin Novedad' tras 90 días.
     */
    @Scheduled(cron = "0 0 3 * * ?") // Ejecuta todos los días a las 3:00 AM
    public void ejecutarDepuracionEvidenciasSinNovedad() {
        log.info("Iniciando tarea programada de depuración de evidencias de patrullajes a 90 días...");
        LocalDateTime ahora = LocalDateTime.now();
        List<Patrullaje> candidatos = patrullajeRepository
                .findByResultadoProcedimientoAndEvidenciaFotoIdNotNullAndEvidenciaDepuradaFalseAndFechaDepuracionEvidenciaBefore(
                        ResultadoProcedimiento.SIN_NOVEDAD, ahora
                );

        int depurados = 0;
        for (Patrullaje p : candidatos) {
            try {
                if (p.getEvidenciaFotoId() != null) {
                    if (ObjectId.isValid(p.getEvidenciaFotoId())) {
                        gridFsTemplate.delete(Query.query(Criteria.where("_id").is(new ObjectId(p.getEvidenciaFotoId()))));
                    } else {
                        gridFsTemplate.delete(Query.query(Criteria.where("_id").is(p.getEvidenciaFotoId())));
                    }
                    p.setEvidenciaDepurada(true);
                    patrullajeRepository.save(p);
                    depurados++;
                }
            } catch (Exception e) {
                log.error("Error al depurar evidencia en GridFS para patrullaje {}: {}", p.getCodigoCorrelativo(), e.getMessage());
            }
        }
        log.info("Depuración de evidencias GridFS completada: {} archivos eliminados.", depurados);
    }

    private PatrullajeDTO mapToDTO(Patrullaje p) {
        String evidenciaUrl = (p.getEvidenciaFotoId() != null && !Boolean.TRUE.equals(p.getEvidenciaDepurada()))
                ? "/api/v1/patrullajes/" + p.getId() + "/evidencia"
                : null;

        return PatrullajeDTO.builder()
                .id(p.getId())
                .codigoCorrelativo(p.getCodigoCorrelativo())
                .latitud(p.getLatitud())
                .longitud(p.getLongitud())
                .coordenadasTexto(p.getCoordenadasTexto())
                .sector(p.getSector())
                .inspectorId(p.getInspectorId())
                .inspectorNombre(p.getInspectorNombre())
                .tipoPatrullaje(p.getTipoPatrullaje())
                .objetivo(p.getObjetivo())
                .instruccionesTacticas(p.getInstruccionesTacticas())
                .tiempoEstimadoMinutos(p.getTiempoEstimadoMinutos())
                .estado(p.getEstado())
                .fechaCreacion(p.getFechaCreacion())
                .fechaAsignacion(p.getFechaAsignacion())
                .fechaLlegadaSitio(p.getFechaLlegadaSitio())
                .fechaCierre(p.getFechaCierre())
                .duracionEfectivaMinutos(p.getDuracionEfectivaMinutos())
                .resultadoProcedimiento(p.getResultadoProcedimiento())
                .observacionesCierre(p.getObservacionesCierre())
                .evidenciaFotoId(p.getEvidenciaFotoId())
                .evidenciaUrl(evidenciaUrl)
                .evidenciaDepurada(p.getEvidenciaDepurada())
                .activo(p.getActivo())
                .build();
    }
}

