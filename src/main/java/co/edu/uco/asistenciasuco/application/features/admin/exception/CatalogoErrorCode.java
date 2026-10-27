package co.edu.uco.asistenciasuco.application.features.admin.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

/**
 * Errores de negocio de la administracion de catalogos (tipos de programa y areas de conocimiento). El codigo
 * coincide con el del catalogo de mensajes (DB / App Configuration), de modo que el usuario recibe el texto
 * institucional y no el generico de CONFLICT o RESOURCE_NOT_FOUND.
 */
public enum CatalogoErrorCode implements ErrorDefinition {
    TPR_001("TPR_001", "El tipo de programa especificado no existe en el sistema.", ErrorKind.NOT_FOUND),
    TPR_002("TPR_002", "Ya existe un tipo de programa con el mismo codigo o nombre.", ErrorKind.CONFLICT),
    ARE_001("ARE_001", "El area de conocimiento especificada no existe en el sistema.", ErrorKind.NOT_FOUND),
    ARE_002("ARE_002", "Ya existe un area de conocimiento con el mismo codigo o nombre.", ErrorKind.CONFLICT);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    CatalogoErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
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
