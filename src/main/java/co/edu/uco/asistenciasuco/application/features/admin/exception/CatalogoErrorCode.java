package co.edu.uco.asistenciasuco.application.features.admin.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

/**
 * Errores de negocio de la administracion de catalogos (tipos de programa, areas de conocimiento y asignaturas). El codigo
 * coincide con el del catalogo de mensajes (DB / App Configuration), de modo que el usuario recibe el texto
 * institucional y no el generico de CONFLICT o RESOURCE_NOT_FOUND.
 */
public enum CatalogoErrorCode implements ErrorDefinition {
    TPR_001("TPR_001", "El tipo de programa especificado no existe en el sistema.", ErrorKind.NOT_FOUND),
    /** DEC-073: los créditos de la asignatura no superan ASIGNATURA/CREDITOS_MAXIMO ({} = máximo vigente). */
    ASI_008("ASI_008", "Los créditos de la asignatura no pueden superar {}.", ErrorKind.VALIDATION),
    /** CULM-001 B2NM: la asignatura la comparten programas que el coordinador no coordina (409). */
    ASI_006("ASI_006", "La asignatura es compartida con programas que no coordinas; solo puedes cambiar su semestre y su carácter en tu plan.",
            ErrorKind.CONFLICT),
    /** HU074 (migración 57): no se inactiva una asignatura con grupos en el período vigente o en períodos futuros (409). */
    ASI_009("ASI_009", "No puedes inactivar la asignatura: tiene grupos en el período vigente o en períodos futuros.", ErrorKind.CONFLICT),
    /** HU074 (migración 57): una asignatura inactiva no se usa para crear grupos ni para asociarla a un plan (409). */
    ASI_010("ASI_010", "La asignatura está inactiva; actívala para usarla.", ErrorKind.CONFLICT),
    TPR_002("TPR_002", "Ya existe un tipo de programa con el mismo codigo o nombre.", ErrorKind.CONFLICT),
    ARE_001("ARE_001", "El area de conocimiento especificada no existe en el sistema.", ErrorKind.NOT_FOUND),
    ARE_002("ARE_002", "Ya existe un area de conocimiento con el mismo codigo o nombre.", ErrorKind.CONFLICT),
    /** Migracion 65: administracion de categorias de justificacion y motivos de cancelacion (texto con los limites que arma la DB). */
    CTG_001("CTG_001", "El campo {} es obligatorio, admite hasta {} caracteres y solo letras, números, espacios y los signos . , - _ ( ) / # :.", ErrorKind.VALIDATION),
    CTG_002("CTG_002", "El código es obligatorio, admite hasta {} caracteres y solo letras mayúsculas, números y guion bajo (_).", ErrorKind.VALIDATION),
    CTG_003("CTG_003", "No se puede desactivar el último registro activo del catálogo de {}: debe quedar al menos uno disponible.", ErrorKind.CONFLICT),
    CTG_004("CTG_004", "La descripción admite hasta {} caracteres y no puede contener los símbolos < ni >.", ErrorKind.VALIDATION),
    CTG_005("CTG_005", "El registro indicado del catálogo de {} no existe.", ErrorKind.NOT_FOUND),
    CTG_006("CTG_006", "Ya existe un registro del catálogo de {} con el mismo {}.", ErrorKind.CONFLICT);

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
