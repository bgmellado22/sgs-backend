package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.model.Catalogo;
import com.conectatech.sgs_backend.model.ParametroSistema;
import com.conectatech.sgs_backend.model.Usuario;
import com.conectatech.sgs_backend.repository.CatalogoRepository;
import com.conectatech.sgs_backend.repository.ParametroSistemaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfiguracionService {

    private final ParametroSistemaRepository parametroSistemaRepository;
    private final CatalogoRepository catalogoRepository;
    private final AuditoriaService auditoriaService;

    /**
     * Tipos de catálogo administrables dinámicamente.
     * Prioridad y Categoría son estándares fijos inmutables.
     */
    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "ORIGEN", "TIPO_INCIDENTE", "UNIDAD_POLICIAL",
            "RESULTADO_PROCEDIMIENTO", "CANAL_ENLACE", "TIPO_PATRULLAJE");

    /**
     * Etiquetas legibles para el mensaje de auditoría por tipo.
     */
    private static final Map<String, String> ETIQUETAS_AUDITORIA = Map.of(
            "ORIGEN", "Origen de denuncia",
            "PRIORIDAD", "Prioridad",
            "CATEGORIA", "Categoría",
            "TIPO_INCIDENTE", "Tipo de Incidente",
            "UNIDAD_POLICIAL", "Unidad Policial",
            "RESULTADO_PROCEDIMIENTO", "Resultado de Procedimiento",
            "CANAL_ENLACE", "Canal de Enlace",
            "TIPO_PATRULLAJE", "Tipo de Patrullaje");

    // ──────────────────────────────────────────────
    // PARÁMETROS DEL SISTEMA (Tiempos de Respuesta y otros)
    // ──────────────────────────────────────────────

    /**
     * Retorna todos los parámetros del sistema.
     */
    public List<ParametroSistema> obtenerParametros() {
        return parametroSistemaRepository.findAll();
    }

    /**
     * Actualización masiva de parámetros.
     * Recibe un mapa clave→valor. Si la clave existe, actualiza; si no, crea el
     * parámetro.
     */
    public List<ParametroSistema> actualizarParametros(Map<String, String> parametros) {
        Usuario admin = auditoriaService.getUsuarioActual();

        parametros.forEach((clave, valor) -> {
            ParametroSistema param = parametroSistemaRepository.findByClave(clave)
                    .orElse(ParametroSistema.builder().clave(clave).build());

            String valorAnterior = param.getValor() == null ? "NO EXISTE" : param.getValor();
            boolean esNuevo = param.getId() == null;

            param.setValor(valor);
            parametroSistemaRepository.save(param);

            String accion = esNuevo ? "CREACIÓN" : "MODIFICACIÓN";
            String mensaje = String.format(
                    "El Administrador %s ha realizado una %s en el Parámetro de Tiempo de Respuesta: %s (Nuevo valor: %s)",
                    admin != null ? admin.getNombreCompleto() : "Sistema",
                    accion,
                    clave,
                    valor);

            String codigoBitacora = esNuevo ? "TIEMPO_RESPUESTA_CREACION" : "TIEMPO_RESPUESTA_MODIFICACION";
            auditoriaService.registrarAuditoria(codigoBitacora, valorAnterior, valor, mensaje);
        });
        return parametroSistemaRepository.findAll();
    }

    /**
     * Elimina un parámetro del sistema por su ID (compatibilidad).
     */
    public void eliminarParametro(String id) {
        eliminarParametro(id, null);
    }

    /**
     * Elimina un parámetro del sistema por su ID con justificación.
     * Gatillo Inalterable: registra automáticamente en la bitácora.
     */
    public void eliminarParametro(String id, String motivo) {
        ParametroSistema parametro = parametroSistemaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Parámetro no encontrado con ID: " + id));

        parametroSistemaRepository.delete(parametro);

        // ── Gatillo Inalterable: Registro Forense ──
        Usuario admin = auditoriaService.getUsuarioActual();

        String sufijoMotivo = (motivo != null && !motivo.isBlank())
                ? ". Motivo: " + motivo.trim()
                : "";

        String mensaje = String.format(
                "El Administrador %s ha eliminado el Parámetro de Tiempo de Respuesta: %s (Valor anterior: %s)%s",
                admin != null ? admin.getNombreCompleto() : "Sistema",
                parametro.getClave(),
                parametro.getValor(),
                sufijoMotivo);

        auditoriaService.registrarAuditoria("TIEMPO_RESPUESTA_ELIMINACION", parametro.getValor(), "ELIMINADO", mensaje);
    }

    // ──────────────────────────────────────────────
    // CATÁLOGOS (Origen, Prioridad, Categoría)
    // con auditoría forense Zero-Trust
    // ──────────────────────────────────────────────

    public List<Catalogo> obtenerCatalogosAdmin(String tipo) {
        String tipoNormalizado = tipo.toUpperCase();
        if (!TIPOS_PERMITIDOS.contains(tipoNormalizado)) {
            throw new IllegalArgumentException(
                    "Tipo de catálogo no permitido: " + tipo
                            + ". Valores válidos: " + TIPOS_PERMITIDOS);
        }
        return catalogoRepository.findByTipo(tipoNormalizado);
    }

    /**
     * Crea un nuevo valor en el catálogo del tipo indicado.
     * Gatillo Inalterable: registra automáticamente en la bitácora
     * la identidad del administrador extraída del SecurityContextHolder.
     *
     * @param tipo     Tipo de catálogo (ORIGEN, PRIORIDAD, CATEGORIA)
     * @param catalogo Datos del nuevo catálogo (valor, etiqueta, nivelOrden,
     *                 colorHex, colorBg)
     * @return Catálogo creado
     * @throws IllegalArgumentException si el tipo no está en la whitelist
     */
    @CacheEvict(value = "catalogos", allEntries = true)
    public Catalogo crearCatalogo(String tipo, Catalogo catalogo) {
        String tipoNormalizado = tipo.toUpperCase();

        // Validación estricta contra whitelist
        if (!TIPOS_PERMITIDOS.contains(tipoNormalizado)) {
            throw new IllegalArgumentException(
                    "Tipo de catálogo no permitido: " + tipo
                            + ". Valores válidos: " + TIPOS_PERMITIDOS);
        }

        // Forzar tipo y activar por defecto
        catalogo.setTipo(tipoNormalizado);
        catalogo.setActivo(true);

        Catalogo guardado = catalogoRepository.save(catalogo);

        // ── Gatillo Inalterable: Registro Forense ──
        Usuario admin = auditoriaService.getUsuarioActual();

        String etiquetaTipo = ETIQUETAS_AUDITORIA.get(tipoNormalizado);
        String nombreOpcion = guardado.getEtiqueta() != null && !guardado.getEtiqueta().isBlank()
                ? guardado.getEtiqueta()
                : guardado.getValor();
        String mensaje = String.format(
                "El Administrador %s ha creado el nuevo %s: %s",
                admin != null ? admin.getNombreCompleto() : "Sistema",
                etiquetaTipo,
                nombreOpcion);

        auditoriaService.registrarAuditoria("CATALOGO_CREACION", null, nombreOpcion, mensaje);

        return guardado;
    }

    /**
     * Edita un catálogo existente.
     * Gatillo Inalterable: registra automáticamente en la bitácora.
     */
    @CacheEvict(value = "catalogos", allEntries = true)
    public Catalogo editarCatalogo(String id, Catalogo catalogoActualizado) {
        Catalogo existente = catalogoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Catálogo no encontrado con ID: " + id));

        if ("PRIORIDAD".equalsIgnoreCase(existente.getTipo()) || "CATEGORIA".equalsIgnoreCase(existente.getTipo())) {
            throw new IllegalArgumentException("Los catálogos estándar de " + existente.getTipo()
                    + " son inmutables por defecto y no pueden modificarse.");
        }

        // Validación de bloqueo optimista
        if (catalogoActualizado.getVersion() != null && existente.getVersion() != null) {
            if (!Objects.equals(existente.getVersion(), catalogoActualizado.getVersion())) {
                throw new org.springframework.dao.OptimisticLockingFailureException(
                        "Conflicto de edición concurrente: Esta opción de catálogo fue modificada por otro administrador.");
            }
            existente.setVersion(catalogoActualizado.getVersion());
        }

        String etiquetaAnterior = existente.getEtiqueta();
        String visualAnterior = describirVisual(existente);
        boolean estadoAnterior = existente.isActivo();

        // El valor interno es inmutable: los incidentes lo guardan como referencia
        // (categoria / prioridad), y cambiarlo los dejaría huérfanos. Solo se edita la
        // etiqueta y estado.
        existente.setEtiqueta(catalogoActualizado.getEtiqueta());
        existente.setActivo(catalogoActualizado.isActivo());

        // Atributos visuales: solo se actualizan si vienen en la petición,
        // para no borrar colores existentes desde formularios que no los envían
        if (catalogoActualizado.getNivelOrden() != null) {
            existente.setNivelOrden(catalogoActualizado.getNivelOrden());
        }
        if (catalogoActualizado.getColorHex() != null) {
            existente.setColorHex(catalogoActualizado.getColorHex());
        }
        if (catalogoActualizado.getColorBg() != null) {
            existente.setColorBg(catalogoActualizado.getColorBg());
        }
        if (catalogoActualizado.getCategoriaAsociada() != null) {
            existente.setCategoriaAsociada(catalogoActualizado.getCategoriaAsociada());
        }
        if (catalogoActualizado.getPrioridadAsociada() != null) {
            existente.setPrioridadAsociada(catalogoActualizado.getPrioridadAsociada());
        }

        Catalogo guardado = catalogoRepository.save(existente);

        // ── Gatillo Inalterable: Registro Forense ──
        Usuario admin = auditoriaService.getUsuarioActual();
        String nombreAdmin = admin != null ? admin.getNombreCompleto() : "Sistema";
        String etiquetaTipo = ETIQUETAS_AUDITORIA.getOrDefault(existente.getTipo(), existente.getTipo());
        String visualNuevo = describirVisual(guardado);
        String sufijoMotivo = (catalogoActualizado.getMotivo() != null && !catalogoActualizado.getMotivo().isBlank())
                ? ". Motivo: " + catalogoActualizado.getMotivo().trim()
                : "";

        // 1. Registrar cambio de estado (Activación/Desactivación)
        if (estadoAnterior != guardado.isActivo()) {
            String accion = guardado.isActivo() ? "activado" : "desactivado";
            String tipoAccion = guardado.isActivo() ? "CATALOGO_ACTIVACION" : "CATALOGO_DESACTIVACION";
            String nombreOpcion = guardado.getEtiqueta() != null && !guardado.getEtiqueta().isBlank()
                    ? guardado.getEtiqueta()
                    : guardado.getValor();
            String mensajeEstado = String.format(
                    "El Administrador %s ha %s el %s: %s%s",
                    nombreAdmin,
                    accion,
                    etiquetaTipo,
                    nombreOpcion,
                    sufijoMotivo);
            auditoriaService.registrarAuditoria(
                    tipoAccion,
                    nombreOpcion + " (" + (estadoAnterior ? "Activo" : "Inactivo") + ")",
                    nombreOpcion + " (" + (guardado.isActivo() ? "Activo" : "Inactivo") + ")",
                    mensajeEstado);
        }

        // 2. Registrar modificación de propiedades SOLO si hubo cambios reales
        boolean cambioEtiqueta = !Objects.equals(etiquetaAnterior, guardado.getEtiqueta());
        boolean cambioVisual = !Objects.equals(visualAnterior, visualNuevo);

        if (cambioEtiqueta || cambioVisual) {
            String difVisual = "";
            if (cambioVisual) {
                if (visualAnterior.isEmpty()) {
                    difVisual = "; Nuevo aspecto: " + visualNuevo;
                } else if (visualNuevo.isEmpty()) {
                    difVisual = "; Aspecto removido";
                } else {
                    difVisual = "; " + visualAnterior + " -> " + visualNuevo;
                }
            }

            String mensajeMod = String.format(
                    "El Administrador %s ha modificado el %s %s (Etiqueta: %s -> %s%s)%s",
                    nombreAdmin,
                    etiquetaTipo,
                    guardado.getValor(),
                    etiquetaAnterior,
                    guardado.getEtiqueta(),
                    difVisual,
                    sufijoMotivo);

            String valorAnt = guardado.getValor() + " (" + etiquetaAnterior
                    + (!visualAnterior.isEmpty() ? ", " + visualAnterior : "") + ")";
            String valorNue = guardado.getValor() + " (" + guardado.getEtiqueta()
                    + (!visualNuevo.isEmpty() ? ", " + visualNuevo : "") + ")";

            auditoriaService.registrarAuditoria("CATALOGO_MODIFICACION", valorAnt, valorNue, mensajeMod);
        }

        return guardado;
    }

    private String describirVisual(Catalogo catalogo) {
        if (catalogo.getNivelOrden() == null && catalogo.getColorHex() == null) {
            return "";
        }
        return "Nivel " + catalogo.getNivelOrden() + ", Color " + catalogo.getColorHex();
    }

    /**
     * Elimina un catálogo por su ID (compatibilidad).
     */
    @CacheEvict(value = "catalogos", allEntries = true)
    public void eliminarCatalogo(String id) {
        eliminarCatalogo(id, null);
    }

    /**
     * Elimina un catálogo por su ID con justificación forense.
     * Gatillo Inalterable: registra automáticamente en la bitácora.
     */
    @CacheEvict(value = "catalogos", allEntries = true)
    public void eliminarCatalogo(String id, String motivo) {
        Catalogo existente = catalogoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Catálogo no encontrado con ID: " + id));

        if ("PRIORIDAD".equalsIgnoreCase(existente.getTipo()) || "CATEGORIA".equalsIgnoreCase(existente.getTipo())) {
            throw new IllegalArgumentException("Los catálogos estándar de " + existente.getTipo()
                    + " son inmutables por defecto y no pueden eliminarse.");
        }

        catalogoRepository.delete(existente);

        // ── Gatillo Inalterable: Registro Forense ──
        Usuario admin = auditoriaService.getUsuarioActual();

        String etiquetaTipo = ETIQUETAS_AUDITORIA.getOrDefault(existente.getTipo(), existente.getTipo());
        String nombreOpcion = existente.getEtiqueta() != null && !existente.getEtiqueta().isBlank()
                ? existente.getEtiqueta()
                : existente.getValor();
        String sufijoMotivo = (motivo != null && !motivo.isBlank())
                ? ". Motivo: " + motivo.trim()
                : "";
        String mensaje = String.format(
                "El Administrador %s ha eliminado el %s: %s%s",
                admin != null ? admin.getNombreCompleto() : "Sistema",
                etiquetaTipo,
                nombreOpcion,
                sufijoMotivo);

        auditoriaService.registrarAuditoria("CATALOGO_ELIMINACION", nombreOpcion, "ELIMINADO", mensaje);
    }
}
