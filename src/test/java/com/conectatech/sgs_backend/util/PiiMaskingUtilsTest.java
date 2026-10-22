package com.conectatech.sgs_backend.util;

import com.conectatech.sgs_backend.dto.DenuncianteDTO;
import com.conectatech.sgs_backend.model.Denunciante;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PiiMaskingUtilsTest {

    @Test
    @DisplayName("Enmascaramiento de RUN debe ofuscar los dígitos centrales")
    public void testMaskRun() {
        assertEquals("12.***.***-5", PiiMaskingUtils.maskRun("12.345.678-5"));
        assertEquals("16.***.***-K", PiiMaskingUtils.maskRun("16.822.441-K"));
        assertNull(PiiMaskingUtils.maskRun(null));
        assertNull(PiiMaskingUtils.maskRun("   "));
    }

    @Test
    @DisplayName("Enmascaramiento de teléfono debe preservar prefijo y últimos 4 dígitos")
    public void testMaskPhone() {
        assertEquals("+56 9 **** 5678", PiiMaskingUtils.maskPhone("+56 9 1234 5678"));
        assertNull(PiiMaskingUtils.maskPhone(null));
        assertNull(PiiMaskingUtils.maskPhone(""));
    }

    @Test
    @DisplayName("Enmascaramiento de email debe ocultar la porción central del identificador")
    public void testMaskEmail() {
        assertEquals("v***o@gmail.com", PiiMaskingUtils.maskEmail("vecino@gmail.com"));
        assertNull(PiiMaskingUtils.maskEmail(null));
    }

    @Test
    @DisplayName("Denunciante con denuncia anónima debe retornar objeto con PII completamente suprimida")
    public void testDenuncianteAnonimo_SuprimePII() {
        Denunciante real = Denunciante.builder()
                .nombres("Juan Carlos")
                .primerApellido("Pérez")
                .segundoApellido("González")
                .telefono("+56912345678")
                .run("11.111.111-1")
                .correoElectronico("juan@correo.cl")
                .enElLugar(true)
                .build();

        DenuncianteDTO anonimoDTO = PiiMaskingUtils.buildSanitizedDenunciante(real, true);

        assertNotNull(anonimoDTO);
        assertEquals("Anónimo", anonimoDTO.getNombres());
        assertNull(anonimoDTO.getPrimerApellido());
        assertNull(anonimoDTO.getSegundoApellido());
        assertNull(anonimoDTO.getTelefono());
        assertNull(anonimoDTO.getRun());
        assertNull(anonimoDTO.getCorreoElectronico());
        assertTrue(anonimoDTO.getEnElLugar());
    }

    @Test
    @DisplayName("Denunciante no anónimo conserva datos según declaración")
    public void testDenuncianteNoAnonimo_ConservaDatos() {
        Denunciante real = Denunciante.builder()
                .nombres("María")
                .primerApellido("Soto")
                .segundoApellido("Lara")
                .telefono("+56987654321")
                .run("12.345.678-5")
                .correoElectronico("maria@correo.cl")
                .enElLugar(false)
                .build();

        DenuncianteDTO dto = PiiMaskingUtils.buildSanitizedDenunciante(real, false);

        assertNotNull(dto);
        assertEquals("María", dto.getNombres());
        assertEquals("Soto", dto.getPrimerApellido());
        assertEquals("12.345.678-5", dto.getRun());
        assertFalse(dto.getEnElLugar());
    }
}
