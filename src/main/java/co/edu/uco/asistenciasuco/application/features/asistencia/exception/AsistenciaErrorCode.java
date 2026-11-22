package co.edu.uco.asistenciasuco.application.features.asistencia.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

public enum AsistenciaErrorCode implements ErrorDefinition {
    ERR_ESTUDIANTE_NO_PERTENECE_SESION("ERR_ESTUDIANTE_NO_PERTENECE_SESION", "El estudiante no pertenece al grupo de la sesion seleccionada.", ErrorKind.FORBIDDEN),
    ERR_ASISTENCIA_REQUERIDA("ERR_ASISTENCIA_REQUERIDA", "La asistencia es obligatoria.", ErrorKind.VALIDATION),
    ERR_PRESENTE_REQUERIDO("ERR_PRESENTE_REQUERIDO", "Debe indicar si el estudiante asistió.", ErrorKind.VALIDATION),
    ERR_SESION_ASISTENCIA_REQUERIDA("ERR_SESION_ASISTENCIA_REQUERIDA", "La sesión de asistencia es obligatoria.", ErrorKind.VALIDATION),
    ERR_REGISTROS_ASISTENCIA_REQUERIDOS("ERR_REGISTROS_ASISTENCIA_REQUERIDOS", "Los registros de asistencia son obligatorios.", ErrorKind.VALIDATION),
    ERR_ESTADO_ASISTENCIA_REQUERIDO("ERR_ESTADO_ASISTENCIA_REQUERIDO", "El estado de asistencia es obligatorio.", ErrorKind.VALIDATION),
    ERR_CODIGO_VERIFICACION_REQUERIDO("ERR_CODIGO_VERIFICACION_REQUERIDO", "El código de verificación es obligatorio.", ErrorKind.VALIDATION),
    ERR_CATEGORIA_REVISION_REQUERIDA("ERR_CATEGORIA_REVISION_REQUERIDA", "La categoría de la revisión es obligatoria.", ErrorKind.VALIDATION),
    ERR_JUSTIFICACION_REVISION_REQUERIDA("ERR_JUSTIFICACION_REVISION_REQUERIDA", "La justificación de la revisión es obligatoria.", ErrorKind.VALIDATION),
    ERR_SOLICITUD_REVISION_REQUERIDA("ERR_SOLICITUD_REVISION_REQUERIDA", "La solicitud de revisión es obligatoria.", ErrorKind.VALIDATION),
    ERR_ACCION_REVISION_REQUERIDA("ERR_ACCION_REVISION_REQUERIDA", "La acción sobre la solicitud es obligatoria.", ErrorKind.VALIDATION),
    // SES-04: resolucion del reclamo de inasistencia (DB: REV_001..REV_005).
    ERR_ACCION_REVISION_INVALIDA("ERR_ACCION_REVISION_INVALIDA", "La acción sobre el reclamo debe ser APROBADA o RECHAZADA.", ErrorKind.VALIDATION),
    /** DEC-073: al rechazar un reclamo la respuesta del docente es obligatoria (10 a 500 caracteres); al aprobar es opcional (hasta 500). */
    ERR_RESPUESTA_RECLAMO_LONGITUD_INVALIDA("ERR_RESPUESTA_RECLAMO_LONGITUD_INVALIDA", "Al rechazar, la respuesta es obligatoria (10 a 500 caracteres); al aprobar es opcional (máx. 500).", ErrorKind.VALIDATION),
    ERR_RECLAMO_YA_RESUELTO("ERR_RECLAMO_YA_RESUELTO", "El reclamo ya fue resuelto con una decisión distinta.", ErrorKind.CONFLICT),
    ERR_RECLAMO_ASISTENCIA_NO_JUSTIFICABLE("ERR_RECLAMO_ASISTENCIA_NO_JUSTIFICABLE", "La asistencia del reclamo no es una inasistencia justificable.", ErrorKind.CONFLICT),
    ERR_RECLAMO_SIN_DETALLE_ASISTENCIA("ERR_RECLAMO_SIN_DETALLE_ASISTENCIA", "El reclamo no tiene un registro de asistencia que se pueda justificar.", ErrorKind.CONFLICT),
    ERR_RECLAMO_NO_EXISTE("ERR_RECLAMO_NO_EXISTE", "El reclamo indicado no existe.", ErrorKind.NOT_FOUND),
    ERR_OBSERVACION_ASISTENCIA_LONGITUD_INVALIDA("ERR_OBSERVACION_ASISTENCIA_LONGITUD_INVALIDA", "Cuando se indique una observación de asistencia, debe tener entre 5 y 250 caracteres.", ErrorKind.VALIDATION),
    ERR_OBSERVACION_ASISTENCIA_REQUERIDA("ERR_OBSERVACION_ASISTENCIA_REQUERIDA", "Debe indicar una observación cuando el estudiante no asiste.", ErrorKind.VALIDATION),
    ERR_MOTIVO_REVISION_REQUERIDO("ERR_MOTIVO_REVISION_REQUERIDO", "El motivo de la revisión es obligatorio.", ErrorKind.VALIDATION),
    ERR_MOTIVO_REVISION_LONGITUD_INVALIDA("ERR_MOTIVO_REVISION_LONGITUD_INVALIDA", "El motivo de la revisión debe tener entre 10 y 300 caracteres.", ErrorKind.VALIDATION),
    ERR_ESTADO_ASISTENCIA_INVALIDO("ERR_ESTADO_ASISTENCIA_INVALIDO", "El estado de asistencia debe ser AN, SJC o EX.", ErrorKind.VALIDATION),
    ERR_USUARIO_EJECUTOR_ASISTENCIA_REQUERIDO("ERR_USUARIO_EJECUTOR_ASISTENCIA_REQUERIDO", "El usuario autenticado que registra las asistencias es obligatorio.", ErrorKind.VALIDATION),
    ERR_DOCENTE_SIN_TITULARIDAD_SESION("ERR_DOCENTE_SIN_TITULARIDAD_SESION", "El docente autenticado no tiene titularidad sobre la sesión indicada.", ErrorKind.FORBIDDEN),
    // SES-01: ventana de registro ordinario [inicio, fin) contra el reloj del servidor; DB: SES_007 / SES_008 / SES_009.
    ERR_SESION_NO_INICIADA("ERR_SESION_NO_INICIADA", "La sesión aún no ha iniciado; la asistencia solo se registra durante la sesión.", ErrorKind.CONFLICT),
    ERR_SESION_CERRADA("ERR_SESION_CERRADA", "La sesión ya finalizó; la asistencia solo puede modificarse mediante un reclamo.", ErrorKind.CONFLICT),
    ERR_SESION_HORARIO_INDETERMINADO("ERR_SESION_HORARIO_INDETERMINADO", "El horario de la sesión no está confirmado; no es posible registrar asistencia.", ErrorKind.CONFLICT),
    // CULM-001 B.1 (NB-1): la sesion fue cancelada (DB: SES_010); no admite registro de asistencia.
    ERR_SESION_CANCELADA("ERR_SESION_CANCELADA", "La sesión fue cancelada; no admite registro de asistencia.", ErrorKind.CONFLICT),
    // CULM-001 D.1 (HU026B): plazo de excusas. DB: REV_020 (vencido, el SP informa la ultima fecha local admitida) y REV_025
    // (faltan ASISTENCIA/DIAS_LIMITE_JUSTIFICACION o TIEMPO/ZONA_HORARIA_SQLSERVER). Java no recalcula el plazo.
    ERR_PLAZO_JUSTIFICACION_VENCIDO("ERR_PLAZO_JUSTIFICACION_VENCIDO", "El plazo para justificar esta inasistencia ya venció.", ErrorKind.CONFLICT),
    ERR_CONFIGURACION_PLAZO_JUSTIFICACION("ERR_CONFIGURACION_PLAZO_JUSTIFICACION", "No es posible validar el plazo de justificación en este momento. Intente más tarde.", ErrorKind.TECHNICAL),
    // CULM-001 D.2 (HU014/HU015): categoria y soportes del reclamo. DB: REV_021 (categoria inexistente), REV_022 (la categoria exige
    // soporte), REV_023 (mas soportes que ARCHIVOS/MAXIMO_SOPORTES_POR_RECLAMO) y REV_024 (soporte repetido). Java no recalcula las reglas.
    ERR_CATEGORIA_JUSTIFICACION_INVALIDA("ERR_CATEGORIA_JUSTIFICACION_INVALIDA", "La categoría de justificación no es válida.", ErrorKind.VALIDATION),
    ERR_SOPORTE_OBLIGATORIO_CATEGORIA("ERR_SOPORTE_OBLIGATORIO_CATEGORIA", "La categoría seleccionada exige adjuntar al menos un soporte.", ErrorKind.VALIDATION),
    ERR_MAXIMO_SOPORTES_RECLAMO("ERR_MAXIMO_SOPORTES_RECLAMO", "Se superó el máximo de soportes permitido por reclamo.", ErrorKind.VALIDATION),
    ERR_SOPORTE_RECLAMO_DUPLICADO("ERR_SOPORTE_RECLAMO_DUPLICADO", "Un mismo soporte no se puede adjuntar dos veces.", ErrorKind.VALIDATION),
    ERR_SOPORTE_RECLAMO_INVALIDO("ERR_SOPORTE_RECLAMO_INVALIDO", "La lista de soportes contiene un identificador no válido.", ErrorKind.VALIDATION),
    // PC (puntos de control): reclamo por toma y resultado de la aprobacion. DB: REV_026 (la toma no es reclamable), REV_027 (ya hay un
    // reclamo de la toma), REV_028 (resultado distinto de PRESENTE/JUSTIFICADA) y REV_029 (aprobar como presente un reclamo sin toma).
    ERR_TOMA_NO_RECLAMABLE("ERR_TOMA_NO_RECLAMABLE", "Solo puedes reclamar una toma en la que quedaste ausente.", ErrorKind.CONFLICT),
    ERR_RECLAMO_TOMA_DUPLICADO("ERR_RECLAMO_TOMA_DUPLICADO", "Ya existe un reclamo para esta toma.", ErrorKind.CONFLICT),
    ERR_RESULTADO_APROBACION_INVALIDO("ERR_RESULTADO_APROBACION_INVALIDO", "El resultado de la aprobación debe ser PRESENTE o JUSTIFICADA.", ErrorKind.VALIDATION),
    ERR_APROBACION_PRESENTE_SIN_TOMA("ERR_APROBACION_PRESENTE_SIN_TOMA", "La aprobación como presente solo aplica a reclamos de una toma.", ErrorKind.CONFLICT);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    AsistenciaErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
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
