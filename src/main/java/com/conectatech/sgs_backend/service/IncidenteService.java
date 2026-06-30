package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.dto.IncidenteRequestDTO;
import com.conectatech.sgs_backend.dto.IncidenteResponseDTO;
import com.conectatech.sgs_backend.model.Incidente;
import com.conectatech.sgs_backend.repository.IncidenteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IncidenteService {
    private final IncidenteRepository incidenteRepository;

    // Guardar
    public IncidenteResponseDTO crearIncidente(IncidenteRequestDTO dto) {
        Incidente incidente = new Incidente();

        incidente.setCategoria(dto.getCategoria());
        incidente.setTipo(dto.getTipo());
        incidente.setDescripcion(dto.getDescripcion());
        incidente.setPrioridad(dto.getPrioridad() != null ? dto.getPrioridad() : "Media");
        incidente.setOrigen(dto.getOrigen());

        incidente.setCodigoCorrelativo("INC-" + (int) (Math.random() * 900 + 100));
        incidente.setEstado("Pendiente");
        incidente.setFechaCreacion(LocalDateTime.now());

        Incidente guardado = incidenteRepository.save(incidente);

        return mapToDTO(guardado);
    }

    // Listar
    public List<IncidenteResponseDTO> obtenerTodos() {
        return incidenteRepository.findByActivoTrue()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // Traducir Entidad a DTO
    private IncidenteResponseDTO mapToDTO(Incidente incidente) {
        IncidenteResponseDTO dto = new IncidenteResponseDTO();
        dto.setId(incidente.getId());
        dto.setCodigoCorrelativo(incidente.getCodigoCorrelativo());
        dto.setCategoria(incidente.getCategoria());
        dto.setTipo(incidente.getTipo());
        dto.setDescripcion(incidente.getDescripcion());
        dto.setPrioridad(incidente.getPrioridad());
        dto.setEstado(incidente.getEstado());
        dto.setFechaCreacion(incidente.getFechaCreacion());
        dto.setOrigen(incidente.getOrigen());
        return dto;
    }

    // Método para actualizar el estado de un incidente
    public IncidenteResponseDTO actualizarEstado(String id, String nuevoEstado) {
        Incidente incidenteExistente = incidenteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Incidente no encontrado con el ID: " + id));

        incidenteExistente.setEstado(nuevoEstado);

        Incidente actualizado = incidenteRepository.save(incidenteExistente);

        return mapToDTO(actualizado);
    }

    // Método para eliminar un incidente
    public void eliminarIncidente(String id) {
        Incidente incidenteExistente = incidenteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Error: No se puede eliminar. Incidente no encontrado con ID: " + id));

        incidenteExistente.setActivo(false);

        incidenteRepository.save(incidenteExistente);
    }
}
