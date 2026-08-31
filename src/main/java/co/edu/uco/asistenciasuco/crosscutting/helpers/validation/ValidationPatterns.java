package co.edu.uco.asistenciasuco.crosscutting.helpers.validation;

import java.util.regex.Pattern;

public final class ValidationPatterns {

    public static final Pattern PERSON_NAME = Pattern.compile("^[\\p{L}\\p{M}]+(?:[ '’-][\\p{L}\\p{M}]+)*$");
    public static final Pattern EMAIL = Pattern.compile("^(?!\\.)(?!.*\\.\\.)[A-Za-z0-9_%+-]+(?:\\.[A-Za-z0-9_%+-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}$");
    public static final Pattern SPACE_SEQUENCE = Pattern.compile(" +");
    public static final Pattern CONTROL_WHITESPACE = Pattern.compile("[\\t\\r\\n]");

    private ValidationPatterns() {
    }
}
