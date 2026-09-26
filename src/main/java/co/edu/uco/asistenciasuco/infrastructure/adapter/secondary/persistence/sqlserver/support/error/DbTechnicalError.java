package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Canal técnico machine-readable publicado por los SP de negocio. */
record DbTechnicalError(String codigo, String detalle) {

    private static final Pattern FORMATO = Pattern.compile("^DBCODE=([A-Z0-9_]+)\\|(.*)$", Pattern.DOTALL);

    static Optional<DbTechnicalError> parse(final String message) {
        if (message == null) {
            return Optional.empty();
        }
        final Matcher matcher = FORMATO.matcher(message);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(new DbTechnicalError(matcher.group(1), matcher.group(2)));
    }

    static boolean hasDbCodeMarker(final String message) {
        return message != null && message.startsWith("DBCODE");
    }
}
