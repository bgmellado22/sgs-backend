package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.model.Sector;
import com.conectatech.sgs_backend.repository.SectorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SectorService {

    public static final String SIN_UBICACION = "Sin ubicación";
    public static final String FUERA_DE_SECTOR = "Fuera de sector";

    // Más allá de esta distancia del sector más cercano, el incidente queda fuera de cobertura
    private static final double DISTANCIA_MAXIMA_KM = 4.0;
    private static final double RADIO_TIERRA_KM = 6371.0;

    private final SectorRepository sectorRepository;

    public List<Sector> obtenerActivos() {
        return sectorRepository.findByActivoTrue();
    }

    /**
     * Retorna el nombre del sector más cercano a la ubicación indicada.
     *
     * @param longitud Longitud del incidente (null si no tiene ubicación)
     * @param latitud  Latitud del incidente (null si no tiene ubicación)
     * @param sectores Sectores activos (se reciben por parámetro para no consultar por cada incidente)
     */
    public String resolverSector(Double longitud, Double latitud, List<Sector> sectores) {
        if (longitud == null || latitud == null) {
            return SIN_UBICACION;
        }

        String masCercano = FUERA_DE_SECTOR;
        double menorDistancia = DISTANCIA_MAXIMA_KM;

        for (Sector sector : sectores) {
            GeoJsonPoint centro = sector.getCentro();
            if (centro == null) {
                continue;
            }
            double distancia = distanciaKm(latitud, longitud, centro.getY(), centro.getX());
            if (distancia <= menorDistancia) {
                menorDistancia = distancia;
                masCercano = sector.getNombre();
            }
        }
        return masCercano;
    }

    // Fórmula de Haversine
    private double distanciaKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return RADIO_TIERRA_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
