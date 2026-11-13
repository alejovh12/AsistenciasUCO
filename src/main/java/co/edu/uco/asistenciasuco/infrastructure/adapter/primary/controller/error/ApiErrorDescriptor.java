package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Error HTTP ya clasificado. {@code messageCode} es el codigo con el que se busca el texto en el catalogo de mensajes y
 * {@code messageArgs} llena sus marcadores (VAL-oleada3 MSG-05); por defecto el codigo es el mismo de la respuesta.
 */
record ApiErrorDescriptor(String code, String message, HttpStatus status, String messageCode, List<String> messageArgs) {

    ApiErrorDescriptor {
        messageCode = messageCode == null ? code : messageCode;
        messageArgs = messageArgs == null ? List.of() : List.copyOf(messageArgs);
    }

    ApiErrorDescriptor(final String code, final String message, final HttpStatus status) {
        this(code, message, status, code, List.of());
    }
}
