package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.model.BitacoraProcedimiento;
import com.conectatech.sgs_backend.model.Catalogo;
import com.conectatech.sgs_backend.model.ParametroSistema;
import com.conectatech.sgs_backend.model.Usuario;
import com.conectatech.sgs_backend.repository.BitacoraProcedimientoRepository;
import com.conectatech.sgs_backend.repository.CatalogoRepository;
import com.conectatech.sgs_backend.repository.ParametroSistemaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfiguracionService {

    private final ParametroSistemaRepository parametroSistemaRepository;
    private final CatalogoRepository catalogoRepository;
    private final BitacoraProcedimientoRepository bitacoraRepository;

    // ──────────────────────────────────────────────
    //  PARÁMETROS DEL SISTEMA (SLAs y otros)
    // ──────────────────────────────────────────────

    /**
     * Retorna todos los parámetros del sistema.
     */
    public List<ParametroSistema> obtenerParametros() {
        return parametroSistemaRepository.findAll();
    }

    /**
     * Actualización masiva de parámetros.
     * Recibe un mapa clave→valor. Si la clave existe, actualiza; si no, crea el parámetro.
     */
    public List<ParametroSistema> actualizarParametros(Map<String, String> parametros) {
        parametros.forEach((clave, valor) -> {
            ParametroSistema param = parametroSistemaRepository.findByClave(clave)
                    .orElse(ParametroSistema.builder().clave(clave).build());
            param.setValor(valor);
            parametroSistemaRepository.save(param);
            log.info("Parámetro actualizado: {} = {}", clave, valor);
        });
        return parametroSistemaRepository.findAll();
    }

    // ──────────────────────────────────────────────
    //  CATÁLOGO DE ORIGEN (con auditoría forense)
    // ──────────────────────────────────────────────

    /**
     * Crea un nuevo valor en el catálogo de tipo ORIGEN.
     * Gatillo Inalterable: registra automáticamente en la bitácora
     * la identidad del administrador extraída del SecurityContextHolder.
     */
    public Catalogo crearOrigen(Catalogo catalogo) {
        // Forzar tipo ORIGEN y activar por defecto
        catalogo.setTipo("ORIGEN");
        catalogo.setActivo(true);

        Catalogo guardado = catalogoRepository.save(catalogo);

        // ── Gatillo Inalterable: Registro Forense ──
        Usuario admin = (Usuario) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        String mensaje = String.format(
                "El Administrador %s ha creado el nuevo Origen de denuncia: %s",
                admin.getNombreCompleto(),
                guardado.getValor()
        );

        BitacoraProcedimiento registro = BitacoraProcedimiento.builder()
                .usuarioId(admin.getId())
                .nombreActor(admin.getNombreCompleto())
                .rolActor(admin.getRol().name())
                .campoModificado("Catálogo Origen")
                .valorAnterior(null)
                .valorNuevo(guardado.getValor())
                .comentario(mensaje)
                .fechaModificacion(LocalDateTime.now())
                .build();

        bitacoraRepository.save(registro);
        log.info("Auditoría forense registrada: {}", mensaje);

        return guardado;
    }
}
