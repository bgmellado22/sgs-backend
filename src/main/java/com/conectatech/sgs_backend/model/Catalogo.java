package com.conectatech.sgs_backend.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
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
    private static final String HEX_REGEX = "^#[0-9A-Fa-f]{6}$";

    @Id
    private String id;
    private String tipo;
    private String valor;
    private String etiqueta;

    // Jerarquía de urgencia (PRIORIDAD) o gravedad (CATEGORIA), del 1 al 5
    @Min(value = 1, message = "El nivel de orden debe estar entre 1 y 5")
    @Max(value = 5, message = "El nivel de orden debe estar entre 1 y 5")
    private Integer nivelOrden;

    // Color del texto / segmento del gráfico, formato #RRGGBB
    @Pattern(regexp = HEX_REGEX, message = "El color debe tener formato #RRGGBB")
    private String colorHex;

    // Color de fondo para badges, formato #RRGGBB
    @Pattern(regexp = HEX_REGEX, message = "El color de fondo debe tener formato #RRGGBB")
    private String colorBg;

    // Relaciones para TIPO_EVENTO -> CATEGORIA y PRIORIDAD
    private String categoriaAsociada;
    private String prioridadAsociada;

    private boolean activo = true;
}
