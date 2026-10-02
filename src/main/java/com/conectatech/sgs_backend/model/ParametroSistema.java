package com.conectatech.sgs_backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "parametros_sistema")
public class ParametroSistema {

    @Id
    private String id;

    @Indexed(unique = true)
    private String clave; // Ej: "SLA_SOS_MINUTOS", "SLA_ALTA_MINUTOS"

    private String valor;
}
