package co.edu.uco.asistenciasuco.crosscutting.validation;

import java.util.regex.Pattern;

public final class ValidationPatterns {

    // Sin cuantificadores anidados (java:S5998): una sola clase de caracteres repetida y las reglas de forma como lookarounds.
    // Letras y marcas separadas por un unico espacio, apostrofe o guion; sin separador al inicio, al final ni repetido.
    public static final Pattern PERSON_NAME = Pattern.compile("^(?![ '’-])(?!.*[ '’-]{2})[\\p{L}\\p{M} '’-]+(?<![ '’-])$");
    // Parte local: [A-Za-z0-9_%+-] con puntos simples (ni al inicio, ni al final, ni dobles). Dominio: etiquetas alfanumericas con guiones
    // internos separadas por puntos, con al menos un punto y TLD de 2 a 63 letras.
    public static final Pattern EMAIL = Pattern.compile("^(?!\\.)(?!.*\\.\\.)[A-Za-z0-9._%+-]+(?<!\\.)@(?![.-])(?!.*(?:-\\.|\\.-))[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$");
    public static final Pattern SPACE_SEQUENCE = Pattern.compile(" +");
    public static final Pattern CONTROL_WHITESPACE = Pattern.compile("[\\t\\r\\n]");

    private ValidationPatterns() {
    }
}
