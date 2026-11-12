package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation;

/**
 * ROL-06: expresiones constantes para {@code @Pattern} de Jakarta Bean Validation en los requests HTTP.
 *
 * <p>Replican las reglas de {@code ValidationPatterns} y de los validators propios, con dos ajustes para que Bean
 * Validation (primera barrera) nunca rechace algo que el validator propio acepta: toleran espacios alrededor (los
 * validators recortan antes de evaluar) y aceptan la cadena vacia en los campos opcionales (la obligatoriedad la
 * declara {@code @NotBlank} aparte).</p>
 */
public final class BeanValidationPatterns {

    /** Correo de {@code ValidationPatterns.EMAIL}, con espacios alrededor o vacio. */
    public static final String EMAIL_OR_BLANK = "^\\s*$|^\\s*(?!\\.)(?!.*\\.\\.)[A-Za-z0-9._%+-]+(?<!\\.)@(?![.-])(?!.*(?:-\\.|\\.-))"
            + "[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}\\s*$";

    /** Token de 43 caracteres base64url (QR de asistencia y de matricula), o vacio. */
    public static final String TOKEN_43_OR_BLANK = "^\\s*$|^\\s*[A-Za-z0-9_-]{43}\\s*$";

    /** Token de 43 caracteres base64url sin espacios (matricula QR no recorta), o vacio. */
    public static final String TOKEN_43_STRICT_OR_BLANK = "^\\s*$|^[A-Za-z0-9_-]{43}$";

    /** PIN de 6 digitos, con espacios alrededor o vacio. */
    public static final String PIN_6_OR_BLANK = "^\\s*$|^\\s*\\d{6}\\s*$";

    /** PIN de 6 digitos sin espacios (matricula QR no recorta), o vacio. */
    public static final String PIN_6_STRICT_OR_BLANK = "^\\s*$|^\\d{6}$";

    /** Solo digitos (numero de identificacion enviado como texto), con espacios alrededor o vacio. */
    public static final String DIGITS_OR_BLANK = "^\\s*$|^\\s*\\d+\\s*$";

    private BeanValidationPatterns() {
    }
}
