package co.edu.uco.asistenciasuco.application.features.cargamasiva.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

/**
 * Codigos de error y rechazo de la carga masiva CSV de usuarios (CULM-001 D.5, CONTRACT §6.3, HU171).
 */
public enum CargaMasivaErrorCode implements ErrorDefinition {
    CMA_001("CMA_001", "El archivo supera el máximo de filas permitidas.", ErrorKind.VALIDATION),
    CMA_002("CMA_002", "El archivo supera el tamaño permitido.", ErrorKind.VALIDATION),
    CMA_003("CMA_003", "El archivo cambió desde la validación. Valídalo de nuevo.", ErrorKind.CONFLICT),
    CMA_004("CMA_004", "La cabecera no coincide con la plantilla.", ErrorKind.VALIDATION),
    CMA_005("CMA_005", "Número de documento inválido.", ErrorKind.VALIDATION),
    CMA_006("CMA_006", "Código de catálogo desconocido.", ErrorKind.VALIDATION),
    CMA_007("CMA_007", "El recurso no está en tu ámbito.", ErrorKind.FORBIDDEN),
    CMA_008("CMA_008", "El documento y el correo corresponden a personas distintas.", ErrorKind.CONFLICT),
    CMA_009("CMA_009", "Tu rol no puede cargar este tipo de cuentas.", ErrorKind.FORBIDDEN),
    CMA_010("CMA_010", "Fila repetida en el archivo.", ErrorKind.VALIDATION),
    CMA_011("CMA_011", "Valor no permitido en la columna.", ErrorKind.VALIDATION),
    CMA_012("CMA_012", "Falta la clave inicial para una persona nueva.", ErrorKind.VALIDATION),
    CMA_013("CMA_013", "El archivo no es un CSV UTF-8 válido.", ErrorKind.VALIDATION),
    CMA_014("CMA_014", "Has superado el número de cargas por minuto.", ErrorKind.TOO_MANY_REQUESTS),
    CMA_015("CMA_015", "Carga procesada correctamente.", ErrorKind.BUSINESS);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    CargaMasivaErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
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
