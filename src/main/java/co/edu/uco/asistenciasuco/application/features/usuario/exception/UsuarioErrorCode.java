package co.edu.uco.asistenciasuco.application.features.usuario.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

public enum UsuarioErrorCode implements ErrorDefinition {
    ERR_UNICIDAD_CORREO("ERR_UNICIDAD_CORREO", "El correo electronico ya se encuentra registrado.", ErrorKind.CONFLICT),
    ERR_UNICIDAD_DOCUMENTO("ERR_UNICIDAD_DOCUMENTO", "El número de documento ya está registrado para este tipo de identificación.", ErrorKind.CONFLICT),
    ERR_IDENTIDAD_USUARIO_CONFLICTO("ERR_IDENTIDAD_USUARIO_CONFLICTO", "El correo y el documento no corresponden al mismo usuario.", ErrorKind.CONFLICT),
    ERR_USUARIO_NO_EXISTE("ERR_USUARIO_NO_EXISTE", "El usuario especificado no existe.", ErrorKind.NOT_FOUND),
    ERR_USUARIO_INACTIVO("ERR_USUARIO_INACTIVO", "El usuario se encuentra inactivo.", ErrorKind.CONFLICT),
    ERR_NOMBRE_PERSONA_INVALIDO("ERR_NOMBRE_PERSONA_INVALIDO", "Los nombres y apellidos solo pueden contener letras, espacios, apóstrofes y guiones.", ErrorKind.VALIDATION),
    ERR_USUARIO_REQUERIDO("ERR_USUARIO_REQUERIDO", "El usuario es obligatorio.", ErrorKind.VALIDATION),
    ERR_USUARIO_INVALIDO("ERR_USUARIO_INVALIDO", "El identificador del usuario no es válido.", ErrorKind.VALIDATION),
    ERR_NUMERO_IDENTIFICACION_REQUERIDO("ERR_NUMERO_IDENTIFICACION_REQUERIDO", "El número de identificación es obligatorio.", ErrorKind.VALIDATION),
    ERR_NUMERO_IDENTIFICACION_INVALIDO("ERR_NUMERO_IDENTIFICACION_INVALIDO", "El número de identificación debe ser positivo y tener entre 6 y 10 dígitos.", ErrorKind.VALIDATION),
    ERR_PRIMER_NOMBRE_REQUERIDO("ERR_PRIMER_NOMBRE_REQUERIDO", "El primer nombre es obligatorio.", ErrorKind.VALIDATION),
    ERR_PRIMER_APELLIDO_REQUERIDO("ERR_PRIMER_APELLIDO_REQUERIDO", "El primer apellido es obligatorio.", ErrorKind.VALIDATION),
    ERR_NOMBRE_PERSONA_LONGITUD_INVALIDA("ERR_NOMBRE_PERSONA_LONGITUD_INVALIDA", "Cada nombre o apellido puede tener máximo 50 caracteres.", ErrorKind.VALIDATION),
    ERR_CORREO_REQUERIDO("ERR_CORREO_REQUERIDO", "El correo es obligatorio.", ErrorKind.VALIDATION),
    ERR_CORREO_LONGITUD_INVALIDA("ERR_CORREO_LONGITUD_INVALIDA", "El correo puede tener máximo 100 caracteres.", ErrorKind.VALIDATION),
    ERR_CORREO_FORMATO_INVALIDO("ERR_CORREO_FORMATO_INVALIDO", "Ingrese un correo válido, por ejemplo nombre@dominio.com.", ErrorKind.VALIDATION),
    ERR_PASSWORD_NUMERO_IDENTIFICACION_REQUERIDO("ERR_PASSWORD_NUMERO_IDENTIFICACION_REQUERIDO", "El número de identificación es obligatorio para validar la clave.", ErrorKind.VALIDATION),
    ERR_PASSWORD_IGUAL_IDENTIFICACION("ERR_PASSWORD_IGUAL_IDENTIFICACION", "La clave no puede ser igual al número de identificación.", ErrorKind.VALIDATION),
    ERR_PASSWORD_REQUERIDO("ERR_PASSWORD_REQUERIDO", "La clave es obligatoria.", ErrorKind.VALIDATION),
    ERR_PASSWORD_POLITICA_INVALIDA("ERR_PASSWORD_POLITICA_INVALIDA", "La clave debe tener mínimo 8 caracteres, con mayúscula, minúscula, número y carácter especial.", ErrorKind.VALIDATION),
    ERR_PASSWORD_LONGITUD_INVALIDA("ERR_PASSWORD_LONGITUD_INVALIDA", "La clave puede tener máximo 255 caracteres.", ErrorKind.VALIDATION),
    ERR_INSTITUCION_INVALIDA("ERR_INSTITUCION_INVALIDA", "El identificador de la institución no es válido.", ErrorKind.VALIDATION),
    ERR_FACULTAD_INVALIDA("ERR_FACULTAD_INVALIDA", "El identificador de la facultad no es válido.", ErrorKind.VALIDATION),
    ERR_PROGRAMA_INVALIDO("ERR_PROGRAMA_INVALIDO", "El identificador del programa no es válido.", ErrorKind.VALIDATION);

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    UsuarioErrorCode(final String code, final String defaultMessage, final ErrorKind kind) {
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
