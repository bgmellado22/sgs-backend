package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.model.Sector;
import com.conectatech.sgs_backend.repository.SectorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class SectorService {

    public static final String SIN_UBICACION = "Sin ubicación";
    public static final String FUERA_DE_SECTOR = "Fuera de sector";

    // Más allá de esta distancia del sector más cercano, el incidente queda fuera de cobertura
    private static final double DISTANCIA_MAXIMA_KM = 4.0;
    private static final double RADIO_TIERRA_KM = 6371.0;

    // Los sectores casi no cambian: se cachean para no consultarlos por cada incidente mapeado
    private static final Duration DURACION_CACHE = Duration.ofMinutes(5);

    private final SectorRepository sectorRepository;
    private final AuditoriaService auditoriaService;

    public SectorService(SectorRepository sectorRepository) {
        this(sectorRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public SectorService(SectorRepository sectorRepository, AuditoriaService auditoriaService) {
        this.sectorRepository = sectorRepository;
        this.auditoriaService = auditoriaService;
    }

    private volatile List<Sector> cache;
    private volatile Instant cacheExpira = Instant.MIN;

    public List<Sector> obtenerActivos() {
        if (cache == null || Instant.now().isAfter(cacheExpira)) {
            cache = List.copyOf(sectorRepository.findByActivoTrue());
            cacheExpira = Instant.now().plus(DURACION_CACHE);
        }
        return cache;
    }

    public List<Sector> obtenerTodos() {
        return sectorRepository.findAll();
    }

    public Sector crearSector(Sector sector) {
        if (sector.getNombre() == null || sector.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del sector es obligatorio.");
        }
        sector.setActivo(true);
        Sector guardado = sectorRepository.save(sector);
        invalidarCache();
        return guardado;
    }

    public Sector actualizarSector(String id, Sector datos) {
        return actualizarSector(id, datos, null);
    }

    public Sector actualizarSector(String id, Sector datos, String motivo) {
        Sector existente = sectorRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Sector no encontrado con ID: " + id));

        boolean estadoAnterior = existente.isActivo();
        String nombreAnterior = existente.getNombre();

        if (datos.getNombre() != null && !datos.getNombre().isBlank()) {
            existente.setNombre(datos.getNombre().trim());
        }
        if (datos.getCentro() != null) {
            existente.setCentro(datos.getCentro());
        }
        existente.setActivo(datos.isActivo());

        Sector guardado = sectorRepository.save(existente);
        invalidarCache();

        if (auditoriaService != null) {
            String sufijoMotivo = (motivo != null && !motivo.isBlank()) ? ". Motivo: " + motivo.trim() : "";
            if (estadoAnterior != guardado.isActivo()) {
                String tipoAccion = guardado.isActivo() ? "SECTOR_ACTIVACION" : "SECTOR_DESACTIVACION";
                auditoriaService.registrarAuditoria(
                        tipoAccion,
                        nombreAnterior + " (" + (estadoAnterior ? "Activo" : "Inactivo") + ")",
                        guardado.getNombre() + " (" + (guardado.isActivo() ? "Activo" : "Inactivo") + ")",
                        "Sector " + guardado.getNombre() + " fue " + (guardado.isActivo() ? "activado" : "desactivado") + sufijoMotivo
                );
            } else if (!nombreAnterior.equals(guardado.getNombre()) || datos.getCentro() != null) {
                auditoriaService.registrarAuditoria(
                        "SECTOR_MODIFICACION",
                        nombreAnterior,
                        guardado.getNombre(),
                        "Modificación de sector comunal " + guardado.getNombre() + sufijoMotivo
                );
            }
        }

        return guardado;
    }

    public void invalidarCache() {
        cache = null;
        cacheExpira = Instant.MIN;
    }

    /**
     * Igual que {@link #resolverSector(Double, Double, List)}, usando los sectores activos cacheados.
     */
    public String resolverSector(Double longitud, Double latitud) {
        return resolverSector(longitud, latitud, obtenerActivos());
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
