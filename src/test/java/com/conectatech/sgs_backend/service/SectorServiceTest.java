package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.model.Sector;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SectorServiceTest {

    private final SectorService service = new SectorService(null);

    private final List<Sector> sectores = List.of(
            new Sector("1", "El Tabo Centro", new GeoJsonPoint(-71.6660, -33.4550), true),
            new Sector("2", "Las Cruces", new GeoJsonPoint(-71.6200, -33.5000), true));

    @Test
    void asignaElSectorMasCercano() {
        assertEquals("El Tabo Centro", service.resolverSector(-71.6650, -33.4560, sectores));
        assertEquals("Las Cruces", service.resolverSector(-71.6220, -33.4980, sectores));
    }

    @Test
    void fueraDeCoberturaSiEstaLejosDeTodoSector() {
        // Santiago centro, ~100 km
        assertEquals(SectorService.FUERA_DE_SECTOR, service.resolverSector(-70.6483, -33.4569, sectores));
    }

    @Test
    void sinUbicacionSiNoHayCoordenadas() {
        assertEquals(SectorService.SIN_UBICACION, service.resolverSector(null, null, sectores));
    }
}
