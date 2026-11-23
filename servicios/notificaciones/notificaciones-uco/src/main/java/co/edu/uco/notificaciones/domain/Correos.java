package co.edu.uco.notificaciones.domain;

/** Reglas de forma de una direccion de correo antes de entregarla al transporte. */
public final class Correos {

    private Correos() {
    }

    /**
     * Valida que sea una direccion unica y segura: rechaza nulos, blancos, saltos de linea, espacios, separadores de lista
     * (, ;), parentesis angulares, comillas y parentesis. Evita inyeccion de cabeceras y listas de destinatarios.
     */
    public static boolean esCorreoSeguro(final String correo) {
        if (correo == null || correo.isBlank()) {
            return false;
        }
        final String trimmed = correo.trim();
        final int atIndex = trimmed.indexOf('@');
        if (atIndex <= 0 || atIndex != trimmed.lastIndexOf('@') || atIndex >= trimmed.length() - 3) {
            return false;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            final char c = trimmed.charAt(i);
            if (Character.isWhitespace(c) || c == ',' || c == ';' || c == '<' || c == '>' || c == '"' || c == '(' || c == ')') {
                return false;
            }
        }
        final String domain = trimmed.substring(atIndex + 1);
        return domain.contains(".") && !domain.startsWith(".") && !domain.endsWith(".");
    }
}
