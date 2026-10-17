package com.conectatech.sgs_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@Document(collection = "incidentes")
public class Incidente {

    @Id
    private String id;

    @Field("codigo_correlativo")
    private String codigoCorrelativo;

    private String categoria;
    private String tipo;
    private String descripcion;
    private String prioridad;
    private String estado;

    @Field("fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Field("fecha_cierre")
    private LocalDateTime fechaCierre;

    @Field("tiempo_resolucion_minutos")
    private Long tiempoResolucionMinutos;

    @Field("sla_minutos_objetivo")
    private Long slaMinutosObjetivo;

    @Field("fecha_vencimiento_sla")
    private LocalDateTime fechaVencimientoSla;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;
    private String direccionTexto;

    private String origen;

    @Field("derivado")
    private boolean derivado;

    @Field("derivacion_entidad")
    private String derivacionEntidad;

    @Field("derivacion_detalle")
    private String derivacionDetalle;

    @Field("derivacion_inspector_id")
    private String derivacionInspectorId;

    @Field("derivacion_inspector_nombre")
    private String derivacionInspectorNombre;

    @Field("derivacion_observacion")
    private String derivacionObservacion;

    @Field("fecha_derivacion")
    private LocalDateTime fechaDerivacion;

    @Field("derivado_por")
    private String derivadoPor;

    @Field("evidencias")
    private List<Evidencia> evidencias;

    @Field("denunciante")
    private Denunciante denunciante;

    @Field("es_anonimo")
    private boolean esAnonimo;

    private boolean activo;

    public Incidente() {
        this.derivado = false;
        this.evidencias = new ArrayList<>();
        this.activo = true;
        this.esAnonimo = true;
    }
}
