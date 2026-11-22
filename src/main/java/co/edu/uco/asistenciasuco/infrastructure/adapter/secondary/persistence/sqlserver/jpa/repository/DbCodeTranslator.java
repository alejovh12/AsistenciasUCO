package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DbExceptionTranslator;
import jakarta.persistence.PersistenceException;

/**
 * Traduce los codigos DBCODE de los SP de consulta (indicadores, verificacion de correo y consultas transversales
 * HU112/HU128/HU143/HU157) a errores de negocio con su HTTP: la DB decide el caso,
 * el backend solo lo clasifica. Los codigos no listados siguen siendo fallos tecnicos (500).
 */
final class DbCodeTranslator {

    private static final String MARCA = "DBCODE=";

    private DbCodeTranslator() {
    }

    static RuntimeException traducir(final PersistenceException exception) {
        return traducir(exception, "estudiante");
    }

    /** {@code entidadNoEncontrada} nombra lo que significa GEN_001 en la consulta que falla (estudiante, programa...). */
    static RuntimeException traducir(final PersistenceException exception, final String entidadNoEncontrada) {
        final String codigo = codigoDe(exception);
        if (codigo == null) {
            return exception;
        }
        return switch (codigo) {
            case "GEN_001" -> ResourceNotFoundException.conParametros(MensajeUsuarioErrorCode.GEN_001, entidadNoEncontrada);
            case "PER_001" -> new ResourceNotFoundException(MensajeUsuarioErrorCode.PER_001);
            case "PAG_001" -> new ValidationException(MensajeUsuarioErrorCode.PAG_001);
            case "GEN_002", "VAL_001" -> new ValidationException(MensajeUsuarioErrorCode.VAL_APP_071);
            // HU112/HU128/HU143/HU157: el SP de lectura exige el perfil del ejecutor.
            case "SEC_001" -> new ForbiddenException(MensajeUsuarioErrorCode.SEC_001);
            // M-01: los demas codigos (KPI_*, VCE_*, ACT_002, MQR_*, FES_001, PAG_001, SEC_002, NTF_002...) salen de la
            // tabla unica de DbFailureClassifier, con el texto del catalogo de mensajes.
            default -> DbExceptionTranslator.translateDbCode(codigo, null).orElse(exception);
        };
    }

    private static String codigoDe(final Throwable exception) {
        for (Throwable actual = exception; actual != null; actual = actual.getCause()) {
            final String mensaje = actual.getMessage();
            if (mensaje != null) {
                final int inicio = mensaje.indexOf(MARCA);
                final int fin = inicio < 0 ? -1 : mensaje.indexOf('|', inicio);
                if (fin > inicio) {
                    return mensaje.substring(inicio + MARCA.length(), fin);
                }
            }
        }
        return null;
    }
}
