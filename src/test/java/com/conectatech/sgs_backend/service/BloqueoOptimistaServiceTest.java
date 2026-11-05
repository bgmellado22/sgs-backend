package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.dto.DerivacionPolicialDTO;
import com.conectatech.sgs_backend.dto.DespachoMunicipalDTO;
import com.conectatech.sgs_backend.dto.IncidenteRequestDTO;
import com.conectatech.sgs_backend.model.Incidente;
import com.conectatech.sgs_backend.repository.IncidenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BloqueoOptimistaServiceTest {

    @Mock
    private IncidenteRepository incidenteRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private IncidenteService incidenteService;

    private Incidente incidenteExistente;

    @BeforeEach
    void setUp() {
        incidenteExistente = new Incidente();
        incidenteExistente.setId("inc-001");
        incidenteExistente.setCodigoCorrelativo("INC-2026-0001");
        incidenteExistente.setEstado("Pendiente");
        incidenteExistente.setVersion(2L); // Versión actual en base de datos es 2
    }

    @Test
    @DisplayName("Debe lanzar OptimisticLockingFailureException al actualizar estado con versión desfasada")
    void actualizarEstado_VersionDesfasada_LanzaOptimisticLockingFailure() {
        when(incidenteRepository.findById("inc-001")).thenReturn(Optional.of(incidenteExistente));

        // El cliente envía version = 1L (desfasada frente a 2L)
        assertThrows(OptimisticLockingFailureException.class, () -> {
            incidenteService.actualizarEstado("inc-001", "Cerrado", "Cierre operativo", 1L);
        });
    }

    @Test
    @DisplayName("Debe lanzar OptimisticLockingFailureException al derivar a Carabineros con versión desfasada")
    void derivarACarabineros_VersionDesfasada_LanzaOptimisticLockingFailure() {
        when(incidenteRepository.findById("inc-001")).thenReturn(Optional.of(incidenteExistente));

        DerivacionPolicialDTO dto = DerivacionPolicialDTO.builder()
                .destacamentoReceptor("2da Comisaría El Tabo")
                .viaComunicacion("Radial")
                .folioDerivacion("CAR-2026-001")
                .antecedentesInformados("Informado por central")
                .version(1L) // Desfasada
                .build();

        assertThrows(OptimisticLockingFailureException.class, () -> {
            incidenteService.derivarACarabineros("inc-001", dto, null);
        });
    }

    @Test
    @DisplayName("Debe lanzar OptimisticLockingFailureException al despachar municipalmente con versión desfasada")
    void despacharMunicipal_VersionDesfasada_LanzaOptimisticLockingFailure() {
        when(incidenteRepository.findById("inc-001")).thenReturn(Optional.of(incidenteExistente));

        DespachoMunicipalDTO dto = DespachoMunicipalDTO.builder()
                .inspectorId("insp-01")
                .inspectorNombre("Juan Pérez")
                .tipoPatrullaje("MUNICIPAL")
                .instrucciones("Patrullar zona costera")
                .version(1L) // Desfasada
                .build();

        assertThrows(OptimisticLockingFailureException.class, () -> {
            incidenteService.despacharMunicipal("inc-001", dto);
        });
    }

    @Test
    @DisplayName("Debe lanzar OptimisticLockingFailureException al editar incidente con versión desfasada")
    void editarIncidente_VersionDesfasada_LanzaOptimisticLockingFailure() {
        when(incidenteRepository.findById("inc-001")).thenReturn(Optional.of(incidenteExistente));

        IncidenteRequestDTO dto = new IncidenteRequestDTO();
        dto.setVersion(1L); // Desfasada
        dto.setCategoria("Seguridad");
        dto.setTipo("Robo");
        dto.setDescripcion("Hechos denunciados");
        dto.setPrioridad("Alta");
        dto.setOrigen("Central Telefónica");

        assertThrows(OptimisticLockingFailureException.class, () -> {
            incidenteService.editarIncidente("inc-001", dto);
        });
    }
}

