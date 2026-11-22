package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.service;

/**
 * Valida que una direccion sea una sola direccion de correo segura: rechaza nulos, blancos, saltos de linea, espacios,
 * separadores de lista (, ;), parentesis angulares, comillas y parentesis. Es la misma regla que aplica el servicio de
 * notificaciones; filtrar aqui evita enviar un aviso que el servicio rechazaria por completo (400 DESTINATARIO_INVALIDO).
 */
final class CorreoSeguro {

    private CorreoSeguro() {
    }

    static boolean esValido(final String correo) {
        if (correo == null || correo.isBlank()) {
            return false;
        }
        final String limpio = correo.strip();
        final int arroba = limpio.indexOf('@');
        if (arroba <= 0 || arroba != limpio.lastIndexOf('@') || arroba >= limpio.length() - 3 || limpio.length() > 254) {
            return false;
        }
        for (int i = 0; i < limpio.length(); i++) {
            final char c = limpio.charAt(i);
            if (Character.isWhitespace(c) || c == ',' || c == ';' || c == '<' || c == '>' || c == '"' || c == '(' || c == ')') {
                return false;
            }
        }
        final String dominio = limpio.substring(arroba + 1);
        return dominio.contains(".") && !dominio.startsWith(".") && !dominio.endsWith(".");
    }
}
