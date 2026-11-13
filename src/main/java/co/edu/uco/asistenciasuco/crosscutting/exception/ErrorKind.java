package co.edu.uco.asistenciasuco.crosscutting.exception;

public enum ErrorKind {
    VALIDATION,
    BUSINESS,
    NOT_FOUND,
    CONFLICT,
    FORBIDDEN,
    UNAUTHORIZED,
    /** CULM-001 B.2: limite de intentos superado (429). */
    TOO_MANY_REQUESTS,
    /** VAL H3 (REA-02/REA-06): una dependencia externa obligatoria (p. ej. el antivirus) no esta disponible (503). */
    SERVICE_UNAVAILABLE,
    TECHNICAL
}
