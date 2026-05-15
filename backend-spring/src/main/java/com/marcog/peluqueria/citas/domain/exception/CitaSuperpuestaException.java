package com.marcog.peluqueria.citas.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class CitaSuperpuestaException extends RuntimeException {
    public CitaSuperpuestaException(String message) {
        super(message);
    }
}
