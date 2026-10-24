package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import jakarta.persistence.PersistenceException;

/**
 * Traduce los codigos DBCODE de los SP de consulta de indicadores a errores de negocio con su HTTP: la DB decide el caso,
 * el backend solo lo clasifica. Los codigos no listados siguen siendo fallos tecnicos (500).
 */
final class DbCodeTranslator {

    private static final String MARCA = "DBCODE=";

    private DbCodeTranslator() {
    }

    static RuntimeException traducir(final PersistenceException exception) {
        final String codigo = codigoDe(exception);
        if (codigo == null) {
            return exception;
        }
        return switch (codigo) {
            case "KPI_001" -> new ForbiddenException("No tiene un programa academico a su cargo para consultar estos indicadores.");
            case "GEN_001" -> new ResourceNotFoundException("El estudiante especificado no existe en el sistema.");
            case "PER_001" -> new ResourceNotFoundException("El periodo academico especificado no existe en el sistema.");
            case "PAG_001" -> new ValidationException("Los parametros de paginacion no son validos.");
            case "GEN_002", "VAL_001" -> new ValidationException("Los datos de la consulta no son validos.");
            default -> exception;
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
