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
    RCN_013("RCN_013", "El programa no tiene coordinador activo para resolver la revisión; solicite su asignación al decano.", ErrorKind.CONFLICT),
    RCN_014("RCN_014", "No puedes resolver una propuesta de cancelación que hiciste como docente.", ErrorKind.CONFLICT),
    /** DEC-065 (1): el decano no resuelve; la decision final es del coordinador del programa. */
    RCN_016("RCN_016", "La resolución de la revisión corresponde al coordinador del programa.", ErrorKind.FORBIDDEN);

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
