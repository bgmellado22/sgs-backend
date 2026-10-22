package com.conectatech.sgs_backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validador para RUN chileno mediante el algoritmo de Módulo 11.
 * Réplica exacta de la lógica de validacionUtils.js del frontend (DRY y Paridad de Seguridad).
 */
public class RutChilenoValidator implements ConstraintValidator<RutChileno, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Se considera válido si está vacío; usar @NotBlank para campos obligatorios
        }

        String runLimpio = value.trim().toUpperCase().replaceAll("[.\\-\\s]", "");

        if (!runLimpio.matches("^[0-9]+[K0-9]$") || runLimpio.length() < 7 || runLimpio.length() > 9) {
            return false;
        }

        String cuerpo = runLimpio.substring(0, runLimpio.length() - 1);
        char dvIngresado = runLimpio.charAt(runLimpio.length() - 1);

        try {
            long cuerpoNum = Long.parseLong(cuerpo);
            if (cuerpoNum >= 50000000L) {
                return false; // Personas naturales (< 50 millones)
            }
        } catch (NumberFormatException e) {
            return false;
        }

        int suma = 0;
        int multiplo = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += Character.getNumericValue(cuerpo.charAt(i)) * multiplo;
            multiplo = multiplo < 7 ? multiplo + 1 : 2;
        }

        int dvEsperado = 11 - (suma % 11);
        char dvCalculado;
        if (dvEsperado == 11) {
            dvCalculado = '0';
        } else if (dvEsperado == 10) {
            dvCalculado = 'K';
        } else {
            dvCalculado = Character.forDigit(dvEsperado, 10);
        }

        return Character.toUpperCase(dvIngresado) == dvCalculado;
    }
}
