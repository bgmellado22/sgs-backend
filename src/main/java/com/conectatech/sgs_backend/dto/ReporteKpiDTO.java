package com.conectatech.sgs_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteKpiDTO {
    private long volumenOperativoTotal;
    private double tasaResolucionEfectiva;
}