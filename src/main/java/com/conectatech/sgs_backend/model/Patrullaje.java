package com.conectatech.sgs_backend.model;

import com.conectatech.sgs_backend.model.enums.EstadoPatrullaje;
import com.conectatech.sgs_backend.model.enums.ResultadoProcedimiento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "patrullajes")
@CompoundIndexes({
    @CompoundIndex(name = "idx_patrullajes_activo_estado_fecha", def = "{'activo': 1, 'estado': 1, 'fechaCreacion': -1}"),
    @CompoundIndex(name = "idx_patrullajes_inspector_estado", def = "{'inspectorId': 1, 'estado': 1}")
})
public class Patrullaje {

    @Id
    private String id;

    @Field("codigo_correlativo")
    private String codigoCorrelativo;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;

    private Double latitud;
    private Double longitud;

    @Field("coordenadas_texto")
    private String coordenadasTexto;

    private String sector;

    @Field("inspector_id")
    private String inspectorId;

    @Field("inspector_nombre")
    private String inspectorNombre;

    @Field("tipo_patrullaje")
    private String tipoPatrullaje;

    private String objetivo;

    @Field("instrucciones_tacticas")
    private String instruccionesTacticas;

    @Field("tiempo_estimado_minutos")
    private Integer tiempoEstimadoMinutos;

    @Builder.Default
    private EstadoPatrullaje estado = EstadoPatrullaje.EN_PROCESO;

    @Field("fecha_creacion")
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Field("fecha_asignacion")
    private LocalDateTime fechaAsignacion;

    @Field("fecha_llegada_sitio")
    private LocalDateTime fechaLlegadaSitio;

    @Field("fecha_cierre")
    private LocalDateTime fechaCierre;

    @Field("duracion_efectiva_minutos")
    private Long duracionEfectivaMinutos;

    @Field("resultado_procedimiento")
    private ResultadoProcedimiento resultadoProcedimiento;

    @Field("observaciones_cierre")
    private String observacionesCierre;

    @Field("evidencia_foto_id")
    private String evidenciaFotoId;

    @Field("fecha_depuracion_evidencia")
    private LocalDateTime fechaDepuracionEvidencia;

    @Field("evidencia_depurada")
    @Builder.Default
    private Boolean evidenciaDepurada = false;

    @Builder.Default
    private Boolean activo = true;
}

