package com.conectatech.sgs_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "catalogos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Catalogo {
    @Id
    private String id;
    private String tipo;
    private String valor;
    private String etiqueta;
    private boolean activo = true;
}
