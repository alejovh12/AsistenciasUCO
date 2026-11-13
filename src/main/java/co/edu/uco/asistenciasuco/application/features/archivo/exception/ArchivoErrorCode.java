package co.edu.uco.asistenciasuco.application.features.archivo.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

/**
 * Codigos de error propios de los soportes (archivos) de reclamos. Coinciden con el catalogo de mensajes de la BD (migracion 42,
 * semilla {@code 63_val_mensajes_err_pendientes.sql}); el texto de aqui es la contingencia ante desconexion del catalogo.
 */
public enum ArchivoErrorCode implements ErrorDefinition {
    /**
     * VAL H3 (REA-02/REA-06): el antivirus no respondio (caido, timeout, respuesta fuera del protocolo o circuit breaker abierto).
     * Falla cerrado: el archivo no se almacena. Se publica como 503 para no confundir la caida de una dependencia con un error del
     * backend.
     */
    ERR_ANTIVIRUS_NO_DISPONIBLE("ERR_ANTIVIRUS_NO_DISPONIBLE",
            "El antivirus no está disponible; el archivo no se guardó. Intente de nuevo en unos minutos.",
            ErrorKind.SERVICE_UNAVAILABLE);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    ArchivoErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.kind = kind;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String defaultMessage() {
        return defaultMessage;
    }

    @Override
    public ErrorKind kind() {
        return kind;
    }
}
