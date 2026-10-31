package com.conectatech.sgs_backend.dto.claveunica;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClaveUnicaUserInfoDTO {

    private String sub;

    @JsonProperty("RolUnico")
    private RolUnicoDTO rolUnico;

    private NameDTO name;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RolUnicoDTO {
        @JsonProperty("DV")
        private String dv;

        private Integer numero;

        private String tipo;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NameDTO {
        private List<String> apellidos;
        private List<String> nombres;
    }

    /**
     * Retorna el RUN en formato estándar con o sin guión según se requiera.
     */
    public String getRunFormateado() {
        if (rolUnico == null || rolUnico.getNumero() == null) {
            return "";
        }
        String dv = rolUnico.getDv() != null ? rolUnico.getDv() : "";
        return rolUnico.getNumero() + "-" + dv;
    }

    public String getNombresString() {
        if (name == null || name.getNombres() == null || name.getNombres().isEmpty()) {
            return "";
        }
        return String.join(" ", name.getNombres()).trim();
    }

    public String getPrimerApellido() {
        if (name == null || name.getApellidos() == null || name.getApellidos().isEmpty()) {
            return "";
        }
        return name.getApellidos().get(0).trim();
    }

    public String getSegundoApellido() {
        if (name == null || name.getApellidos() == null || name.getApellidos().size() < 2) {
            return null;
        }
        return name.getApellidos().get(1).trim();
    }

    public String getNombreCompleto() {
        String nombres = getNombresString();
        String primer = getPrimerApellido();
        String segundo = getSegundoApellido();

        StringBuilder sb = new StringBuilder();
        if (!nombres.isEmpty()) sb.append(nombres);
        if (!primer.isEmpty()) sb.append(sb.length() > 0 ? " " : "").append(primer);
        if (segundo != null && !segundo.isEmpty()) sb.append(" ").append(segundo);
        return sb.toString().trim();
    }
}

