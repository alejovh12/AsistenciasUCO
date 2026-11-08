package co.edu.uco.asistenciasuco.application.features.alertainasistencia.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

/** CULM-001 C.1 (HU026C): codigos de las alertas de inasistencia que llegan a HTTP; el texto sale del catalogo (ALR_*). */
public enum AlertaInasistenciaErrorCode implements ErrorDefinition {
    ALR_004("ALR_004", "La lista de alertas no es válida.", ErrorKind.VALIDATION),
    ALR_006("ALR_006", "La matrícula indicada no pertenece al grupo.", ErrorKind.VALIDATION);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    AlertaInasistenciaErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
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
