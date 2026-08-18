package co.edu.uco.asistenciasuco.application.features.usuario.domain.rules;

import co.edu.uco.asistenciasuco.application.exception.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.TextHelper;

public final class PasswordRegistroRule {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 255;
    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=%";

    private PasswordRegistroRule() {
        throw new IllegalStateException("No es permitido instanciar una regla de dominio utilitaria.");
    }

    public static String resolver(
            final String password,
            final Integer numeroIdentificacion
    ) {
        if (numeroIdentificacion == null) {
            throw new ValidationException(
                    "ERR_PASSWORD_NUMERO_IDENTIFICACION_REQUERIDO",
                    "El numero de identificacion es obligatorio para resolver el password."
            );
        }
        if (password == null) {
            return null;
        }
        if (String.valueOf(numeroIdentificacion).equals(password)) {
            throw new ValidationException(
                    "ERR_PASSWORD_IGUAL_IDENTIFICACION",
                    "El password no puede ser igual al numero de identificacion."
            );
        }
        validarPasswordExplicito(password);
        return password;
    }

    public static String resolverCredencialNueva(
            final String password,
            final Integer numeroIdentificacion
    ) {
        if (password == null) {
            throw new ValidationException(
                    "ERR_PASSWORD_REQUERIDO",
                    "El password es obligatorio para crear una credencial nueva."
            );
        }
        return resolver(password, numeroIdentificacion);
    }

    private static void validarPasswordExplicito(final String valor) {
        validarLongitud(valor);
        if (TextHelper.isNullOrBlank(valor) || TextHelper.containsWhitespace(valor) || !cumplePolitica(valor)) {
            throw new ValidationException(
                    "ERR_PASSWORD_POLITICA_INVALIDA",
                    "El password no cumple la politica definida."
            );
        }
    }

    private static void validarLongitud(final String valor) {
        if (valor.length() > MAX_LENGTH) {
            throw new ValidationException(
                    "ERR_PASSWORD_LONGITUD_INVALIDA",
                    "El password debe tener maximo 255 caracteres."
            );
        }
    }

    private static boolean cumplePolitica(final String valor) {
        return valor.length() >= MIN_LENGTH
                && valor.chars().anyMatch(Character::isDigit)
                && valor.chars().anyMatch(Character::isUpperCase)
                && valor.chars().anyMatch(Character::isLowerCase)
                && valor.chars().anyMatch(character -> SPECIAL_CHARS.indexOf(character) >= 0);
    }
}
