package co.edu.uco.asistenciasuco.crosscutting.validation;

import java.util.regex.Pattern;

public final class ValidationPatterns {

    // Sin cuantificadores anidados (java:S5998): una sola clase de caracteres repetida y las reglas de forma como lookarounds.
    // Letras y marcas separadas por un unico espacio, apostrofe o guion; sin separador al inicio, al final ni repetido.
    public static final Pattern PERSON_NAME = Pattern.compile("^(?![ '’-])(?!.*[ '’-]{2})[\\p{L}\\p{M} '’-]+(?<![ '’-])$");
    // Parte local: [A-Za-z0-9_%+-] con puntos simples (ni al inicio, ni al final, ni dobles). Dominio: etiquetas alfanumericas con guiones
    // internos separadas por puntos, con al menos un punto y TLD de 2 a 63 letras.
    public static final Pattern EMAIL = Pattern.compile("^(?!\\.)(?!.*\\.\\.)[A-Za-z0-9._%+-]+(?<!\\.)@(?![.-])(?!.*(?:-\\.|\\.-))[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$");
    // DEC-073: nombre o codigo de una entidad (facultad, programa, periodo, grupo, sesion, asignatura). Una sola clase de
    // caracteres: letras y marcas, digitos, espacio y la puntuacion . , - _ ( ) / # :. Sin < >, comillas ni control.
    public static final Pattern SAFE_TEXT = Pattern.compile("^[\\p{L}\\p{M}\\p{N} .,_()/#:-]+$");
    // DEC-073: texto libre (respuestas, justificaciones): se admite todo salvo los delimitadores de etiquetas HTML.
    public static final Pattern HTML_TAG_DELIMITER = Pattern.compile("[<>]");
    // DEC-073: numero de identificacion recibido como texto: solo digitos, sin cero inicial, hasta 10 cifras.
    public static final Pattern IDENTIFICATION_DIGITS = Pattern.compile("^[1-9]\\d{0,9}$");
    public static final Pattern SPACE_SEQUENCE = Pattern.compile(" +");
    public static final Pattern CONTROL_WHITESPACE = Pattern.compile("[\\t\\r\\n]");

    private ValidationPatterns() {
    }
}
