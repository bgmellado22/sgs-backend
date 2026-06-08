package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.model.Incidente;
import com.conectatech.sgs_backend.repository.IncidenteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidenteService {
    private final IncidenteRepository incidenteRepository;

    public List<Incidente> obtenerTodos() {
        return incidenteRepository.findAll();
    }

    public Incidente crearIncidente(Incidente nuevoIncidente) {
        // Todo incidente nuevo debe registrar la hora exacta del servidor
        nuevoIncidente.setFechaCreacion(LocalDateTime.now());

        return incidenteRepository.save(nuevoIncidente);
    }

    public List<Incidente> obtenerPorEstado(String estado) {
        return incidenteRepository.findByEstado(estado);
    }
}
