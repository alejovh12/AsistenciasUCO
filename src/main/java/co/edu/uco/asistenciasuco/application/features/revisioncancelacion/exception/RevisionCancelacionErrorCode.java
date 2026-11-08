package co.edu.uco.asistenciasuco.application.features.revisioncancelacion.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

/** CULM-001 C.1 (HU026C): errores de la revision de cancelacion por inasistencia (el codigo de la DB se expone tal cual). */
public enum RevisionCancelacionErrorCode implements ErrorDefinition {
    RCN_001("RCN_001", "La revisión de cancelación no existe.", ErrorKind.NOT_FOUND),
    RCN_002("RCN_002", "La decisión no es válida.", ErrorKind.VALIDATION),
    RCN_003("RCN_003", "El motivo no tiene la longitud permitida.", ErrorKind.VALIDATION),
    RCN_004("RCN_004", "La revisión no admite esa decisión en su estado actual.", ErrorKind.CONFLICT),
    RCN_005("RCN_005", "La matrícula del estudiante ya no está activa en el grupo.", ErrorKind.CONFLICT),
    RCN_006("RCN_006", "El estudiante ya no supera el límite de inasistencia.", ErrorKind.CONFLICT),
    RCN_012("RCN_012", "Hay casos de pérdida por inasistencia pendientes; resuélvelos antes del cierre.", ErrorKind.CONFLICT),
    RCN_013("RCN_013", "No hay un coordinador ni un decano activo que pueda resolver la propuesta.", ErrorKind.CONFLICT),
    RCN_014("RCN_014", "No puedes resolver una propuesta de cancelación que hiciste como docente.", ErrorKind.CONFLICT);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    RevisionCancelacionErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
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
