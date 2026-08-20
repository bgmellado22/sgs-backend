package com.conectatech.sgs_backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CredencialesIncorrectasException extends ResponseStatusException {

    public CredencialesIncorrectasException(String mensaje) {
        super(HttpStatus.UNAUTHORIZED, mensaje);
    }
}
