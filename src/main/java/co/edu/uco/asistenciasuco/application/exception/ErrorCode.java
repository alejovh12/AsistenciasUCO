package co.edu.uco.asistenciasuco.application.exception;

import co.edu.uco.asistenciasuco.crosscutting.helpers.TextHelper;

import java.util.Arrays;
import java.util.Optional;

public enum ErrorCode {
    VALIDATION_ERROR("VALIDATION_ERROR", "La solicitud no es valida.", ErrorKind.VALIDATION),
    INVALID_REQUEST("INVALID_REQUEST", "La solicitud no es valida.", ErrorKind.VALIDATION),
    BUSINESS_ERROR("BUSINESS_ERROR", "No fue posible completar la operacion solicitada.", ErrorKind.BUSINESS),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "El recurso solicitado no existe.", ErrorKind.NOT_FOUND),
    CONFLICT("CONFLICT", "La operacion genera un conflicto con el estado actual.", ErrorKind.CONFLICT),
    FORBIDDEN("FORBIDDEN", "No tiene permisos para realizar esta operacion.", ErrorKind.FORBIDDEN),
    UNAUTHORIZED("UNAUTHORIZED", "Debe autenticarse para realizar esta operacion.", ErrorKind.UNAUTHORIZED),
    INTERNAL_ERROR("INTERNAL_ERROR", "Ocurrio un error interno. Utilice el codigo de seguimiento para soporte.", ErrorKind.TECHNICAL),
    INTERNAL_APPLICATION_ERROR("INTERNAL_APPLICATION_ERROR", "Ocurrio un error interno. Utilice el codigo de seguimiento para soporte.", ErrorKind.TECHNICAL),
    DATABASE_OPERATION_ERROR("DATABASE_OPERATION_ERROR", "Ocurrio un error interno. Utilice el codigo de seguimiento para soporte.", ErrorKind.TECHNICAL),
    ERR_DB_UNCLASSIFIED("ERR_DB_UNCLASSIFIED", "Ocurrio un error interno. Utilice el codigo de seguimiento para soporte.", ErrorKind.TECHNICAL),

    ERR_MATRICULA_DUPLICADA("ERR_MATRICULA_DUPLICADA", "La matricula ya se encuentra registrada para este grupo.", ErrorKind.CONFLICT),
    ERR_CUPO_SUPERADO("ERR_CUPO_SUPERADO", "No hay cupos disponibles para el grupo.", ErrorKind.CONFLICT),
    ERR_CRUCE_HORARIO_ESTUDIANTE("ERR_CRUCE_HORARIO_ESTUDIANTE", "Existe cruce de horario para el estudiante.", ErrorKind.CONFLICT),
    ERR_CRUCE_HORARIO_DOCENTE("ERR_CRUCE_HORARIO_DOCENTE", "Existe cruce de horario para el docente.", ErrorKind.CONFLICT),
    ERR_UNICIDAD_CORREO("ERR_UNICIDAD_CORREO", "El correo ya se encuentra registrado.", ErrorKind.CONFLICT),
    ERR_UNICIDAD_DOCUMENTO("ERR_UNICIDAD_DOCUMENTO", "El numero de identificacion ya se encuentra registrado.", ErrorKind.CONFLICT),
    ERR_GRUPO_NO_EXISTE("ERR_GRUPO_NO_EXISTE", "El grupo consultado no existe.", ErrorKind.NOT_FOUND),
    ERR_GRUPO_NO_HABILITADO("ERR_GRUPO_NO_HABILITADO", "El grupo no se encuentra habilitado.", ErrorKind.CONFLICT),
    ERR_TIPO_IDENTIFICACION_NO_EXISTE("ERR_TIPO_IDENTIFICACION_NO_EXISTE", "El tipo de identificacion no existe.", ErrorKind.NOT_FOUND),
    ERR_ESTUDIANTE_NO_EXISTE("ERR_ESTUDIANTE_NO_EXISTE", "El estudiante consultado no existe.", ErrorKind.NOT_FOUND),
    ERR_USUARIO_NO_EXISTE("ERR_USUARIO_NO_EXISTE", "El usuario consultado no existe.", ErrorKind.NOT_FOUND),
    ERR_DOCENTE_NO_EXISTE("ERR_DOCENTE_NO_EXISTE", "El docente consultado no existe.", ErrorKind.NOT_FOUND),
    ERR_SESION_NO_EXISTE("ERR_SESION_NO_EXISTE", "La sesion consultada no existe.", ErrorKind.NOT_FOUND),
    ERR_ESTUDIANTE_NO_PERTENECE_SESION("ERR_ESTUDIANTE_NO_PERTENECE_SESION", "El estudiante no pertenece a la sesion.", ErrorKind.FORBIDDEN),
    ERR_USUARIO_INACTIVO("ERR_USUARIO_INACTIVO", "El usuario se encuentra inactivo.", ErrorKind.CONFLICT),
    ERR_ESTUDIANTE_INACTIVO("ERR_ESTUDIANTE_INACTIVO", "El estudiante se encuentra inactivo.", ErrorKind.CONFLICT),
    ERR_DOCENTE_INACTIVO("ERR_DOCENTE_INACTIVO", "El docente se encuentra inactivo.", ErrorKind.CONFLICT),
    ERR_DOCENTE_YA_REGISTRADO("ERR_DOCENTE_YA_REGISTRADO", "El usuario ya se encuentra registrado como docente.", ErrorKind.CONFLICT),
    ERR_NOMBRE_PERSONA_INVALIDO("ERR_NOMBRE_PERSONA_INVALIDO", "El nombre contiene caracteres no permitidos.", ErrorKind.VALIDATION),
    ERR_IDENTIDAD_USUARIO_CONFLICTO("ERR_IDENTIDAD_USUARIO_CONFLICTO", "La identidad del usuario no coincide con el estudiante.", ErrorKind.CONFLICT),

    ERR_DOCENTE_REQUERIDO("ERR_DOCENTE_REQUERIDO", "El docente es obligatorio.", ErrorKind.VALIDATION),
    ERR_DOCENTE_INVALIDO("ERR_DOCENTE_INVALIDO", "Debe seleccionar un docente valido.", ErrorKind.VALIDATION),
    ERR_GRUPO_REQUERIDO("ERR_GRUPO_REQUERIDO", "El grupo es obligatorio.", ErrorKind.VALIDATION),
    ERR_GRUPO_INVALIDO("ERR_GRUPO_INVALIDO", "Debe seleccionar un grupo valido.", ErrorKind.VALIDATION),
    ERR_SESION_REQUERIDA("ERR_SESION_REQUERIDA", "La sesion es obligatoria.", ErrorKind.VALIDATION),
    ERR_ASISTENCIA_REQUERIDA("ERR_ASISTENCIA_REQUERIDA", "La asistencia es obligatoria.", ErrorKind.VALIDATION),
    ERR_PRESENTE_REQUERIDO("ERR_PRESENTE_REQUERIDO", "El indicador de presencia es obligatorio.", ErrorKind.VALIDATION),
    ERR_OBSERVACION_ASISTENCIA_LONGITUD_INVALIDA("ERR_OBSERVACION_ASISTENCIA_LONGITUD_INVALIDA", "La observacion de la asistencia no cumple la longitud permitida.", ErrorKind.VALIDATION),
    ERR_OBSERVACION_ASISTENCIA_REQUERIDA("ERR_OBSERVACION_ASISTENCIA_REQUERIDA", "Debe indicar una observacion cuando el estudiante no asiste.", ErrorKind.VALIDATION),
    ERR_MOTIVO_REVISION_REQUERIDO("ERR_MOTIVO_REVISION_REQUERIDO", "El motivo de revision es obligatorio.", ErrorKind.VALIDATION),
    ERR_MOTIVO_REVISION_LONGITUD_INVALIDA("ERR_MOTIVO_REVISION_LONGITUD_INVALIDA", "El motivo de revision no cumple la longitud permitida.", ErrorKind.VALIDATION),
    ERR_TEMA_SESION_REQUERIDO("ERR_TEMA_SESION_REQUERIDO", "El tema de la sesion es obligatorio.", ErrorKind.VALIDATION),
    ERR_TEMA_SESION_LONGITUD_INVALIDA("ERR_TEMA_SESION_LONGITUD_INVALIDA", "El tema de la sesion no cumple la longitud permitida.", ErrorKind.VALIDATION),
    ERR_DESCRIPCION_SESION_LONGITUD_INVALIDA("ERR_DESCRIPCION_SESION_LONGITUD_INVALIDA", "La descripcion de la sesion no cumple la longitud permitida.", ErrorKind.VALIDATION),
    ERR_OBSERVACION_CIERRE_REQUERIDA("ERR_OBSERVACION_CIERRE_REQUERIDA", "La observacion de cierre es obligatoria.", ErrorKind.VALIDATION),
    ERR_OBSERVACION_CIERRE_LONGITUD_INVALIDA("ERR_OBSERVACION_CIERRE_LONGITUD_INVALIDA", "La observacion de cierre no cumple la longitud permitida.", ErrorKind.VALIDATION),
    ERR_USUARIO_REQUERIDO("ERR_USUARIO_REQUERIDO", "El usuario es obligatorio.", ErrorKind.VALIDATION),
    ERR_USUARIO_INVALIDO("ERR_USUARIO_INVALIDO", "El usuario debe ser valido.", ErrorKind.VALIDATION),
    ERR_USUARIO_DOCENTE_REQUERIDO("ERR_USUARIO_DOCENTE_REQUERIDO", "El usuario del docente es obligatorio.", ErrorKind.VALIDATION),
    ERR_USUARIO_DOCENTE_INVALIDO("ERR_USUARIO_DOCENTE_INVALIDO", "El usuario del docente debe ser valido.", ErrorKind.VALIDATION),
    ERR_NUMERO_IDENTIFICACION_DOCENTE_REQUERIDO("ERR_NUMERO_IDENTIFICACION_DOCENTE_REQUERIDO", "El numero de identificacion del docente es obligatorio.", ErrorKind.VALIDATION),
    ERR_NOMBRE_COMPLETO_DOCENTE_REQUERIDO("ERR_NOMBRE_COMPLETO_DOCENTE_REQUERIDO", "El nombre completo del docente es obligatorio.", ErrorKind.VALIDATION),
    ERR_ESTUDIANTE_ID_REQUERIDO("ERR_ESTUDIANTE_ID_REQUERIDO", "El estudiante es obligatorio.", ErrorKind.VALIDATION),
    ERR_ESTUDIANTE_ID_INVALIDO("ERR_ESTUDIANTE_ID_INVALIDO", "Debe seleccionar un estudiante valido.", ErrorKind.VALIDATION),
    ERR_PAGE_INVALIDA("ERR_PAGE_INVALIDA", "La pagina debe ser mayor o igual que cero.", ErrorKind.VALIDATION),
    ERR_SIZE_INVALIDO("ERR_SIZE_INVALIDO", "El tamano de pagina debe estar entre 1 y 100.", ErrorKind.VALIDATION),
    ERR_TIPO_IDENTIFICACION_INVALIDA("ERR_TIPO_IDENTIFICACION_INVALIDA", "El tipo de identificacion no es valido.", ErrorKind.VALIDATION),
    ERR_TIPO_IDENTIFICACION_CODIGO_REQUERIDO("ERR_TIPO_IDENTIFICACION_CODIGO_REQUERIDO", "El codigo del tipo de identificacion es obligatorio.", ErrorKind.VALIDATION),
    ERR_TIPO_IDENTIFICACION_CODIGO_LONGITUD_INVALIDA("ERR_TIPO_IDENTIFICACION_CODIGO_LONGITUD_INVALIDA", "El codigo del tipo de identificacion no cumple la longitud permitida.", ErrorKind.VALIDATION),
    ERR_TIPO_IDENTIFICACION_NOMBRE_REQUERIDO("ERR_TIPO_IDENTIFICACION_NOMBRE_REQUERIDO", "El nombre del tipo de identificacion es obligatorio.", ErrorKind.VALIDATION),
    ERR_TIPO_IDENTIFICACION_NOMBRE_LONGITUD_INVALIDA("ERR_TIPO_IDENTIFICACION_NOMBRE_LONGITUD_INVALIDA", "El nombre del tipo de identificacion no cumple la longitud permitida.", ErrorKind.VALIDATION),
    ERR_INSTITUCION_INVALIDA("ERR_INSTITUCION_INVALIDA", "La institucion no es valida.", ErrorKind.VALIDATION),
    ERR_FACULTAD_INVALIDA("ERR_FACULTAD_INVALIDA", "La facultad no es valida.", ErrorKind.VALIDATION),
    ERR_PROGRAMA_INVALIDO("ERR_PROGRAMA_INVALIDO", "El programa no es valido.", ErrorKind.VALIDATION),
    ERR_TIPO_IDENTIFICACION_REQUERIDA("ERR_TIPO_IDENTIFICACION_REQUERIDA", "El tipo de identificacion es obligatorio.", ErrorKind.VALIDATION),
    ERR_NUMERO_IDENTIFICACION_REQUERIDO("ERR_NUMERO_IDENTIFICACION_REQUERIDO", "El numero de identificacion es obligatorio.", ErrorKind.VALIDATION),
    ERR_NUMERO_IDENTIFICACION_INVALIDO("ERR_NUMERO_IDENTIFICACION_INVALIDO", "El numero de identificacion no es valido.", ErrorKind.VALIDATION),
    ERR_PRIMER_NOMBRE_REQUERIDO("ERR_PRIMER_NOMBRE_REQUERIDO", "El primer nombre es obligatorio.", ErrorKind.VALIDATION),
    ERR_PRIMER_APELLIDO_REQUERIDO("ERR_PRIMER_APELLIDO_REQUERIDO", "El primer apellido es obligatorio.", ErrorKind.VALIDATION),
    ERR_NOMBRE_PERSONA_LONGITUD_INVALIDA("ERR_NOMBRE_PERSONA_LONGITUD_INVALIDA", "El nombre no cumple la longitud permitida.", ErrorKind.VALIDATION),
    ERR_CORREO_REQUERIDO("ERR_CORREO_REQUERIDO", "El correo es obligatorio.", ErrorKind.VALIDATION),
    ERR_CORREO_LONGITUD_INVALIDA("ERR_CORREO_LONGITUD_INVALIDA", "El correo no cumple la longitud permitida.", ErrorKind.VALIDATION),
    ERR_CORREO_FORMATO_INVALIDO("ERR_CORREO_FORMATO_INVALIDO", "El correo no tiene un formato valido.", ErrorKind.VALIDATION),
    ERR_PASSWORD_NUMERO_IDENTIFICACION_REQUERIDO("ERR_PASSWORD_NUMERO_IDENTIFICACION_REQUERIDO", "El numero de identificacion es obligatorio para validar la clave.", ErrorKind.VALIDATION),
    ERR_PASSWORD_IGUAL_IDENTIFICACION("ERR_PASSWORD_IGUAL_IDENTIFICACION", "La clave no puede ser igual al numero de identificacion.", ErrorKind.VALIDATION),
    ERR_PASSWORD_REQUERIDO("ERR_PASSWORD_REQUERIDO", "La clave es obligatoria.", ErrorKind.VALIDATION),
    ERR_PASSWORD_POLITICA_INVALIDA("ERR_PASSWORD_POLITICA_INVALIDA", "La clave no cumple la politica definida.", ErrorKind.VALIDATION),
    ERR_PASSWORD_LONGITUD_INVALIDA("ERR_PASSWORD_LONGITUD_INVALIDA", "La clave no cumple la longitud permitida.", ErrorKind.VALIDATION);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    ErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.kind = kind;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    public ErrorKind kind() {
        return kind;
    }

    public static Optional<ErrorCode> fromCode(final String code) {
        if (TextHelper.isNullOrBlank(code)) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(errorCode -> errorCode.code.equals(code.trim()))
                .findFirst();
    }
}
