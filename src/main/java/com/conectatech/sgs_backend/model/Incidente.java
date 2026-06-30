package com.conectatech.sgs_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
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

    private GeoJsonPoint ubicacion;

    private String origen;

    private boolean activo = true;
}
