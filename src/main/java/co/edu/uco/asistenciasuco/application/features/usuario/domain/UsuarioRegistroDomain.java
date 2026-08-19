package co.edu.uco.asistenciasuco.application.features.usuario.domain;

import co.edu.uco.asistenciasuco.application.exception.ErrorCode;
import co.edu.uco.asistenciasuco.application.exception.ValidationException;
import co.edu.uco.asistenciasuco.application.features.usuario.domain.rules.PasswordRegistroRule;
import co.edu.uco.asistenciasuco.crosscutting.helpers.TextHelper;

import java.util.UUID;

/**
 * Reglas compartidas para registrar datos base de usuario.
 */
public final class UsuarioRegistroDomain {

    private static final UUID EMPTY_UUID = new UUID(0L, 0L);
    private static final int MIN_DIGITS_NUMERO_IDENTIFICACION = 6;
    private static final int MAX_DIGITS_NUMERO_IDENTIFICACION = 10;
    private static final int MAX_LENGTH_NOMBRE_PERSONA = 50;
    private static final int MAX_LENGTH_CORREO = 100;

    private final UUID tipoIdentificacionId;
    private final Integer numeroIdentificacion;
    private final String primerApellido;
    private final String segundoApellido;
    private final String primerNombre;
    private final String segundoNombre;
    private final String correo;
    private final String password;

    private UsuarioRegistroDomain(
            final UUID tipoIdentificacionId,
            final Integer numeroIdentificacion,
            final String primerApellido,
            final String segundoApellido,
            final String primerNombre,
            final String segundoNombre,
            final String correo,
            final String password
    ) {
        this.tipoIdentificacionId = tipoIdentificacionId;
        this.numeroIdentificacion = numeroIdentificacion;
        this.primerApellido = primerApellido;
        this.segundoApellido = segundoApellido;
        this.primerNombre = primerNombre;
        this.segundoNombre = segundoNombre;
        this.correo = correo;
        this.password = password;
    }

    public static UsuarioRegistroDomain crear(
            final UUID tipoIdentificacionId,
            final Integer numeroIdentificacion,
            final String primerApellido,
            final String segundoApellido,
            final String primerNombre,
            final String segundoNombre,
            final String correo,
            final String password
    ) {
        final Integer numeroIdentificacionValidado = validarNumeroIdentificacion(numeroIdentificacion);
        return new UsuarioRegistroDomain(
                validarTipoIdentificacion(tipoIdentificacionId),
                numeroIdentificacionValidado,
                validarNombreObligatorio(
                        primerApellido,
                        ErrorCode.ERR_PRIMER_APELLIDO_REQUERIDO
                ),
                validarNombreOpcional(segundoApellido),
                validarNombreObligatorio(
                        primerNombre,
                        ErrorCode.ERR_PRIMER_NOMBRE_REQUERIDO
                ),
                validarNombreOpcional(segundoNombre),
                validarCorreo(correo),
                PasswordRegistroRule.resolver(password, numeroIdentificacionValidado)
        );
    }

    private static UUID validarTipoIdentificacion(final UUID tipoIdentificacionId) {
        if (tipoIdentificacionId == null) {
            throw new ValidationException(ErrorCode.ERR_TIPO_IDENTIFICACION_REQUERIDA);
        }
        if (EMPTY_UUID.equals(tipoIdentificacionId)) {
            throw new ValidationException(ErrorCode.ERR_TIPO_IDENTIFICACION_INVALIDA);
        }
        return tipoIdentificacionId;
    }

    private static Integer validarNumeroIdentificacion(final Integer numeroIdentificacion) {
        if (numeroIdentificacion == null) {
            throw new ValidationException(ErrorCode.ERR_NUMERO_IDENTIFICACION_REQUERIDO);
        }
        if (numeroIdentificacion <= 0) {
            throw new ValidationException(ErrorCode.ERR_NUMERO_IDENTIFICACION_INVALIDO);
        }
        final int digits = String.valueOf(numeroIdentificacion).length();
        if (digits < MIN_DIGITS_NUMERO_IDENTIFICACION || digits > MAX_DIGITS_NUMERO_IDENTIFICACION) {
            throw new ValidationException(ErrorCode.ERR_NUMERO_IDENTIFICACION_INVALIDO);
        }
        return numeroIdentificacion;
    }

    private static String validarNombreObligatorio(
            final String valor,
            final ErrorCode requiredCode
    ) {
        final String normalizado = TextHelper.normalizeTrimUpper(valor);
        if (TextHelper.isNullOrBlank(normalizado)) {
            throw new ValidationException(requiredCode);
        }
        return validarLongitudNombre(normalizado);
    }

    private static String validarNombreOpcional(final String valor) {
        return validarLongitudNombre(TextHelper.normalizeTrimUpperToEmpty(valor));
    }

    private static String validarLongitudNombre(final String valor) {
        if (valor.length() > MAX_LENGTH_NOMBRE_PERSONA) {
            throw new ValidationException(ErrorCode.ERR_NOMBRE_PERSONA_LONGITUD_INVALIDA);
        }
        return valor;
    }

    private static String validarCorreo(final String correo) {
        final String normalizado = TextHelper.normalizeTrimLower(correo);
        if (TextHelper.isNullOrBlank(normalizado)) {
            throw new ValidationException(ErrorCode.ERR_CORREO_REQUERIDO);
        }
        if (normalizado.length() > MAX_LENGTH_CORREO) {
            throw new ValidationException(ErrorCode.ERR_CORREO_LONGITUD_INVALIDA);
        }
        try {
            return TextHelper.requireEmailFormat(normalizado, "El correo no tiene un formato valido.");
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(ErrorCode.ERR_CORREO_FORMATO_INVALIDO, exception.getMessage());
        }
    }

    public UUID getTipoIdentificacionId() {
        return tipoIdentificacionId;
    }

    public Integer getNumeroIdentificacion() {
        return numeroIdentificacion;
    }

    public String getPrimerApellido() {
        return primerApellido;
    }

    public String getSegundoApellido() {
        return segundoApellido;
    }

    public String getPrimerNombre() {
        return primerNombre;
    }

    public String getSegundoNombre() {
        return segundoNombre;
    }

    public String getCorreo() {
        return correo;
    }

    public String getPassword() {
        return password;
    }
}
