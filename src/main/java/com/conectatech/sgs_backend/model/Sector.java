package com.conectatech.sgs_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Localidad o sector comunal. Cada incidente se asigna al sector
 * cuyo punto central esté más cerca de su ubicación.
 */
@Document(collection = "sectores")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sector {
    @Id
    private String id;
    private String nombre;
    private GeoJsonPoint centro;
    private boolean activo = true;
}
