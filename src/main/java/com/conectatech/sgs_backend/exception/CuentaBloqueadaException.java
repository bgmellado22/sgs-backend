package com.conectatech.sgs_backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CuentaBloqueadaException extends ResponseStatusException {

    public CuentaBloqueadaException(String mensaje) {
        super(HttpStatus.TOO_MANY_REQUESTS, mensaje);
    }
}
