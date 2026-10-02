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
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfiguracionService {

    private final ParametroSistemaRepository parametroSistemaRepository;
    private final CatalogoRepository catalogoRepository;
    private final BitacoraProcedimientoRepository bitacoraRepository;

    /**
     * Tipos de catálogo permitidos para creación dinámica.
     */
    private static final Set<String> TIPOS_PERMITIDOS = Set.of("ORIGEN", "PRIORIDAD", "CATEGORIA");

    /**
     * Etiquetas legibles para el mensaje de auditoría por tipo.
     */
    private static final Map<String, String> ETIQUETAS_AUDITORIA = Map.of(
            "ORIGEN", "Origen de denuncia",
            "PRIORIDAD", "Prioridad",
            "CATEGORIA", "Categoría"
    );

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
    //  CATÁLOGOS (Origen, Prioridad, Categoría)
    //  con auditoría forense Zero-Trust
    // ──────────────────────────────────────────────

    /**
     * Crea un nuevo valor en el catálogo del tipo indicado.
     * Gatillo Inalterable: registra automáticamente en la bitácora
     * la identidad del administrador extraída del SecurityContextHolder.
     *
     * @param tipo     Tipo de catálogo (ORIGEN, PRIORIDAD, CATEGORIA)
     * @param catalogo Datos del nuevo catálogo (valor, etiqueta)
     * @return Catálogo creado
     * @throws IllegalArgumentException si el tipo no está en la whitelist
     */
    public Catalogo crearCatalogo(String tipo, Catalogo catalogo) {
        String tipoNormalizado = tipo.toUpperCase();

        // Validación estricta contra whitelist
        if (!TIPOS_PERMITIDOS.contains(tipoNormalizado)) {
            throw new IllegalArgumentException(
                    "Tipo de catálogo no permitido: " + tipo
                            + ". Valores válidos: " + TIPOS_PERMITIDOS
            );
        }

        // Forzar tipo y activar por defecto
        catalogo.setTipo(tipoNormalizado);
        catalogo.setActivo(true);

        Catalogo guardado = catalogoRepository.save(catalogo);

        // ── Gatillo Inalterable: Registro Forense ──
        Usuario admin = (Usuario) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        String etiquetaTipo = ETIQUETAS_AUDITORIA.get(tipoNormalizado);
        String mensaje = String.format(
                "El Administrador %s ha creado el nuevo %s: %s",
                admin.getNombreCompleto(),
                etiquetaTipo,
                guardado.getValor()
        );

        BitacoraProcedimiento registro = BitacoraProcedimiento.builder()
                .usuarioId(admin.getId())
                .nombreActor(admin.getNombreCompleto())
                .rolActor(admin.getRol().name())
                .campoModificado("Catálogo " + etiquetaTipo)
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
