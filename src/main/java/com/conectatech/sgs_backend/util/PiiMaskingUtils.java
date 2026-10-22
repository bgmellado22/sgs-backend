package com.conectatech.sgs_backend.util;

import com.conectatech.sgs_backend.dto.DenuncianteDTO;
import com.conectatech.sgs_backend.model.Denunciante;

/**
 * Utilidades para Enmascaramiento y Protección de Datos Personales (PII).
 * Cumplimiento con ISO/IEC 27001:2022 (A.8.11 Enmascaramiento de datos, A.5.34 Privacidad)
 * y Ley N° 19.628 sobre Protección de la Vida Privada.
 */
public final class PiiMaskingUtils {

    private PiiMaskingUtils() {
        // Utility class
    }

    /**
     * Ofusca un RUN chileno dejando visibles los primeros dígitos y el dígito verificador.
     * Ejemplo: "12.345.678-9" -> "12.***.***-9"
     */
    public static String maskRun(String run) {
        if (run == null || run.trim().isEmpty()) {
            return null;
        }
        String limpio = run.trim();
        int guionIdx = limpio.lastIndexOf('-');
        if (guionIdx > 2) {
            String cuerpo = limpio.substring(0, guionIdx);
            String dv = limpio.substring(guionIdx);
            String visible = cuerpo.length() > 2 ? cuerpo.substring(0, 2) : cuerpo;
            return visible + ".***.***" + dv;
        }
        return "***-X";
    }

    /**
     * Ofusca un número de teléfono preservando el prefijo y los últimos dígitos.
     * Ejemplo: "+56 9 1234 5678" -> "+56 9 **** 5678"
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        String limpio = phone.trim();
        if (limpio.length() > 6) {
            String inicio = limpio.substring(0, Math.min(6, limpio.length() - 4));
            String fin = limpio.substring(limpio.length() - 4);
            return inicio + " **** " + fin;
        }
        return "****";
    }

    /**
     * Ofusca un correo electrónico ocultando la parte central del nombre de usuario.
     * Ejemplo: "vecino.eltabo@gmail.com" -> "v***o@gmail.com"
     */
    public static String maskEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        String limpio = email.trim();
        int atIdx = limpio.indexOf('@');
        if (atIdx > 2) {
            String usuario = limpio.substring(0, atIdx);
            String dominio = limpio.substring(atIdx);
            return usuario.charAt(0) + "***" + usuario.charAt(usuario.length() - 1) + dominio;
        } else if (atIdx > 0) {
            return limpio.charAt(0) + "***" + limpio.substring(atIdx);
        }
        return "***@***";
    }

    /**
     * Mapea y enmascara los datos de un denunciante según la política de anonimato y privacidad.
     * Si la denuncia fue declarada como anónima, se suprimen todos los datos de contacto y filiación.
     */
    public static DenuncianteDTO buildSanitizedDenunciante(Denunciante denunciante, boolean esAnonimo) {
        if (denunciante == null) {
            return null;
        }

        if (esAnonimo) {
            return DenuncianteDTO.builder()
                    .nombres("Anónimo")
                    .primerApellido(null)
                    .segundoApellido(null)
                    .telefono(null)
                    .run(null)
                    .correoElectronico(null)
                    .enElLugar(denunciante.getEnElLugar())
                    .build();
        }

        return DenuncianteDTO.builder()
                .nombres(denunciante.getNombres())
                .primerApellido(denunciante.getPrimerApellido())
                .segundoApellido(denunciante.getSegundoApellido())
                .telefono(denunciante.getTelefono())
                .run(denunciante.getRun())
                .correoElectronico(denunciante.getCorreoElectronico())
                .enElLugar(denunciante.getEnElLugar())
                .build();
    }
}
