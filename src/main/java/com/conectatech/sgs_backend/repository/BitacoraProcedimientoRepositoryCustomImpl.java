package com.conectatech.sgs_backend.repository;

import com.conectatech.sgs_backend.model.BitacoraProcedimiento;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Repository
@RequiredArgsConstructor
public class BitacoraProcedimientoRepositoryCustomImpl implements BitacoraProcedimientoRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<BitacoraProcedimiento> buscarConFiltrosPaginado(
            String textoBusqueda,
            String modulo,
            String tipoOperacion,
            String rolActor,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            Pageable pageable) {

        Query query = new Query();
        List<Criteria> criterios = new ArrayList<>();

        // Búsqueda por texto libre
        if (textoBusqueda != null && !textoBusqueda.isBlank()) {
            String regex = Pattern.quote(textoBusqueda.trim());
            Criteria textoCriteria = new Criteria().orOperator(
                    Criteria.where("nombreActor").regex(regex, "i"),
                    Criteria.where("campoModificado").regex(regex, "i"),
                    Criteria.where("codigoCorrelativo").regex(regex, "i"),
                    Criteria.where("incidenteId").regex(regex, "i"),
                    Criteria.where("comentario").regex(regex, "i"),
                    Criteria.where("valorAnterior").regex(regex, "i"),
                    Criteria.where("valorNuevo").regex(regex, "i")
            );
            criterios.add(textoCriteria);
        }

        // Filtro por Módulo
        if (modulo != null && !modulo.isBlank()) {
            switch (modulo.toUpperCase().trim()) {
                case "TIEMPO_RESPUESTA":
                case "SLA":
                    criterios.add(Criteria.where("campoModificado").regex("TIEMPO_RESPUESTA|SLA", "i"));
                    break;
                case "CATALOGOS":
                    criterios.add(Criteria.where("campoModificado").regex("CATALOGO|CATÁLOGO", "i"));
                    break;
                case "USUARIOS":
                    criterios.add(Criteria.where("campoModificado").regex("USUARIO|REGISTRO|LOGIN|LOGOUT|SESION|SESIÓN|AUTENTICACION|AUTENTICACIÓN", "i"));
                    break;
                case "INCIDENTES":
                    Criteria incidenteCriteria = new Criteria().orOperator(
                            Criteria.where("incidenteId").ne(null),
                            Criteria.where("codigoCorrelativo").ne(null),
                            Criteria.where("campoModificado").in(
                                    "Creación", "Evidencia", "Estado", "Prioridad", "Origen",
                                    "Categoría", "Tipo", "Descripción", "Dirección", "Eliminación",
                                    "Derivación Operativa", "creación", "eliminación"
                            )
                    );
                    criterios.add(incidenteCriteria);
                    break;
                default:
                    break;
            }
        }

        // Filtro por Tipo de Operación
        if (tipoOperacion != null && !tipoOperacion.isBlank()) {
            switch (tipoOperacion.toUpperCase().trim()) {
                case "CREACION":
                    criterios.add(Criteria.where("campoModificado").regex("CREACION|CREACIÓN|REGISTRO", "i"));
                    break;
                case "ELIMINACION":
                    criterios.add(Criteria.where("campoModificado").regex("ELIMINACION|ELIMINACIÓN|DESACTIVACION|DESACTIVACIÓN", "i"));
                    break;
                case "MODIFICACION":
                    criterios.add(Criteria.where("campoModificado").not().regex("CREACION|CREACIÓN|REGISTRO|ELIMINACION|ELIMINACIÓN|DESACTIVACION|DESACTIVACIÓN|LOGIN|LOGOUT|SESION|SESIÓN|AUTENTICACION|AUTENTICACIÓN", "i"));
                    break;
                case "SESION":
                case "AUTENTICACION":
                case "LOGIN_LOGOUT":
                case "LOGIN":
                    criterios.add(Criteria.where("campoModificado").regex("LOGIN|LOGOUT|SESION|SESIÓN|AUTENTICACION|AUTENTICACIÓN", "i"));
                    break;
                default:
                    break;
            }
        }

        // Filtro por Rol del Actor
        if (rolActor != null && !rolActor.isBlank()) {
            criterios.add(Criteria.where("rolActor").regex("^" + Pattern.quote(rolActor.trim()) + "$", "i"));
        }

        // Filtro por Rango de Fechas
        if (fechaInicio != null && fechaFin != null) {
            criterios.add(Criteria.where("fechaModificacion").gte(fechaInicio).lte(fechaFin));
        } else if (fechaInicio != null) {
            criterios.add(Criteria.where("fechaModificacion").gte(fechaInicio));
        } else if (fechaFin != null) {
            criterios.add(Criteria.where("fechaModificacion").lte(fechaFin));
        }

        if (!criterios.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criterios.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query, BitacoraProcedimiento.class);
        query.with(pageable);
        List<BitacoraProcedimiento> resultados = mongoTemplate.find(query, BitacoraProcedimiento.class);

        return new PageImpl<>(resultados, pageable, total);
    }
}

