package com.conectatech.sgs_backend.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RutChilenoValidatorTest {

    private RutChilenoValidator validator;

    @BeforeEach
    public void setUp() {
        validator = new RutChilenoValidator();
    }

    @Test
    @DisplayName("RUNs válidos con o sin formato deben ser aceptados")
    public void testRutValido_RetornaTrue() {
        assertTrue(validator.isValid("11.111.111-1", null));
        assertTrue(validator.isValid("11111111-1", null));
        assertTrue(validator.isValid("111111111", null));
        assertTrue(validator.isValid("12.345.678-5", null));
        assertTrue(validator.isValid("12345678-5", null));
        // Caso con dígito verificador K (11.111.112-K)
        assertTrue(validator.isValid("11.111.112-K", null));
        assertTrue(validator.isValid("11111112-k", null));
    }

    @Test
    @DisplayName("Valores nulos o vacíos se consideran válidos (la obligatoriedad la define @NotBlank)")
    public void testRutVacioONulo_RetornaTrue() {
        assertTrue(validator.isValid(null, null));
        assertTrue(validator.isValid("", null));
        assertTrue(validator.isValid("   ", null));
    }

    @Test
    @DisplayName("RUNs con dígito verificador incorrecto deben ser rechazados")
    public void testRutDigitoVerificadorInvalido_RetornaFalse() {
        assertFalse(validator.isValid("11.111.111-2", null));
        assertFalse(validator.isValid("12.345.678-9", null));
        assertFalse(validator.isValid("16822441-0", null));
    }

    @Test
    @DisplayName("RUNs de personas jurídicas (>= 50 millones) deben ser rechazados")
    public void testRutPersonaJuridica_RetornaFalse() {
        assertFalse(validator.isValid("76.123.456-7", null));
        assertFalse(validator.isValid("65.000.000-8", null));
    }

    @Test
    @DisplayName("Formatos no numéricos o longitudes absurdas deben ser rechazados")
    public void testRutFormatoInvalido_RetornaFalse() {
        assertFalse(validator.isValid("abcdef", null));
        assertFalse(validator.isValid("123", null));
        assertFalse(validator.isValid("1234567890123", null));
    }
}
