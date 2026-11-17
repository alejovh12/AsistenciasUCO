package co.edu.uco.asistenciasuco.application.features.admin.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

/**
 * FIX-CIERRE-002: errores de negocio de los periodos academicos que publica la base (el codigo de la DB se expone tal cual).
 * El texto con la fecha de fin lo arma {@code usp_obtener_mensaje_catalogo}; el de aqui es solo el respaldo.
 */
public enum PeriodoAcademicoErrorCode implements ErrorDefinition {
    PER_007("PER_007",
            "El período académico aún no ha finalizado; el cierre masivo solo puede ejecutarse después de su fecha de fin.",
            ErrorKind.CONFLICT);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    PeriodoAcademicoErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
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
