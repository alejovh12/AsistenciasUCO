package co.edu.uco.asistenciasuco.application.features.usuario.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;

public enum UsuarioErrorCode implements ErrorDefinition {
    ERR_UNICIDAD_CORREO("ERR_UNICIDAD_CORREO", "El correo ya se encuentra registrado.", ErrorKind.CONFLICT),
    ERR_UNICIDAD_DOCUMENTO("ERR_UNICIDAD_DOCUMENTO", "El numero de identificacion ya se encuentra registrado.", ErrorKind.CONFLICT),
    ERR_USUARIO_NO_EXISTE("ERR_USUARIO_NO_EXISTE", "El usuario consultado no existe.", ErrorKind.NOT_FOUND),
    ERR_USUARIO_INACTIVO("ERR_USUARIO_INACTIVO", "El usuario se encuentra inactivo.", ErrorKind.CONFLICT),
    ERR_NOMBRE_PERSONA_INVALIDO("ERR_NOMBRE_PERSONA_INVALIDO", "Los nombres y apellidos solo pueden contener letras, espacios, apostrofes y guiones.", ErrorKind.VALIDATION),
    ERR_USUARIO_REQUERIDO("ERR_USUARIO_REQUERIDO", "El usuario es obligatorio.", ErrorKind.VALIDATION),
    ERR_USUARIO_INVALIDO("ERR_USUARIO_INVALIDO", "El identificador del usuario no es valido.", ErrorKind.VALIDATION),
    ERR_NUMERO_IDENTIFICACION_REQUERIDO("ERR_NUMERO_IDENTIFICACION_REQUERIDO", "El numero de identificacion es obligatorio.", ErrorKind.VALIDATION),
    ERR_NUMERO_IDENTIFICACION_INVALIDO("ERR_NUMERO_IDENTIFICACION_INVALIDO", "El numero de identificacion debe ser positivo y contener entre 6 y 10 digitos.", ErrorKind.VALIDATION),
    ERR_PRIMER_NOMBRE_REQUERIDO("ERR_PRIMER_NOMBRE_REQUERIDO", "El primer nombre es obligatorio.", ErrorKind.VALIDATION),
    ERR_PRIMER_APELLIDO_REQUERIDO("ERR_PRIMER_APELLIDO_REQUERIDO", "El primer apellido es obligatorio.", ErrorKind.VALIDATION),
    ERR_NOMBRE_PERSONA_LONGITUD_INVALIDA("ERR_NOMBRE_PERSONA_LONGITUD_INVALIDA", "Cada nombre o apellido puede tener maximo 50 caracteres.", ErrorKind.VALIDATION),
    ERR_CORREO_REQUERIDO("ERR_CORREO_REQUERIDO", "El correo es obligatorio.", ErrorKind.VALIDATION),
    ERR_CORREO_LONGITUD_INVALIDA("ERR_CORREO_LONGITUD_INVALIDA", "El correo puede tener maximo 100 caracteres.", ErrorKind.VALIDATION),
    ERR_CORREO_FORMATO_INVALIDO("ERR_CORREO_FORMATO_INVALIDO", "Ingrese un correo valido, por ejemplo nombre@dominio.com.", ErrorKind.VALIDATION),
    ERR_PASSWORD_NUMERO_IDENTIFICACION_REQUERIDO("ERR_PASSWORD_NUMERO_IDENTIFICACION_REQUERIDO", "El numero de identificacion es obligatorio para validar la clave.", ErrorKind.VALIDATION),
    ERR_PASSWORD_IGUAL_IDENTIFICACION("ERR_PASSWORD_IGUAL_IDENTIFICACION", "La clave no puede ser igual al numero de identificacion.", ErrorKind.VALIDATION),
    ERR_PASSWORD_REQUERIDO("ERR_PASSWORD_REQUERIDO", "La clave es obligatoria.", ErrorKind.VALIDATION),
    ERR_PASSWORD_POLITICA_INVALIDA("ERR_PASSWORD_POLITICA_INVALIDA", "La clave debe tener al menos 8 caracteres e incluir una mayuscula, una minuscula, un numero y un caracter especial.", ErrorKind.VALIDATION),
    ERR_PASSWORD_LONGITUD_INVALIDA("ERR_PASSWORD_LONGITUD_INVALIDA", "La clave puede tener maximo 255 caracteres.", ErrorKind.VALIDATION),
    ERR_INSTITUCION_INVALIDA("ERR_INSTITUCION_INVALIDA", "El identificador de la institucion no es valido.", ErrorKind.VALIDATION),
    ERR_FACULTAD_INVALIDA("ERR_FACULTAD_INVALIDA", "El identificador de la facultad no es valido.", ErrorKind.VALIDATION),
    ERR_PROGRAMA_INVALIDO("ERR_PROGRAMA_INVALIDO", "El identificador del programa no es valido.", ErrorKind.VALIDATION);

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
