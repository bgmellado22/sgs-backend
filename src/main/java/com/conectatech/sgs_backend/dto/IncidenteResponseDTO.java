package com.conectatech.sgs_backend.dto;

import com.conectatech.sgs_backend.model.Evidencia;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class IncidenteResponseDTO {
    private String id;
    private Long version;
    private String codigoCorrelativo;
    private String categoria;
    private String tipo;
    private String descripcion;
    private String prioridad;
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaCierre;
    private Long tiempoResolucionMinutos;
    private Long tiempoRespuestaMinutosObjetivo;
    private LocalDateTime fechaVencimientoTiempoRespuesta;
    private String origen;
    private Double latitud;
    private Double longitud;
    private String direccionTexto;
    private String sector; // Localidad más cercana (calculada, no persistida)

    // Gestión Operativa (Derivación y Despacho)
    private boolean derivado;
    private String derivacionEntidad;
    private String derivacionDetalle;
    private String derivacionInspectorId;
    private String derivacionInspectorNombre;
    private String derivacionObservacion;
    private LocalDateTime fechaDerivacion;
    private String derivadoPor;
    private String destacamentoReceptor;
    private String viaComunicacion;
    private String folioDerivacion;
    private String comprobantePdfId;
    private String tipoPatrullaje;

    // Evidencias Multimedia
    private List<Evidencia> evidencias;

    // Datos del Denunciante
    private DenuncianteDTO denunciante;
    private boolean esAnonimo;

    // Resolución de Cierre y Notificación
    private String resolucionMunicipal;
    private Boolean notificadoCierreEmail;
    private LocalDateTime fechaNotificacionCierre;

    // Estado lógico
    private boolean activo;
}
