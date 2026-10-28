package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query;

import java.util.UUID;

/**
 * Conversiones de columnas de una fila {@code Object[]} devuelta por una consulta nativa (EXEC de un SP).
 *
 * <p>El driver de SQL Server entrega {@code UNIQUEIDENTIFIER} como {@link String} (no como {@link UUID}) cuando el
 * resultado no pasa por un mapeo de entidad: un cast directo a {@code UUID} lanza {@link ClassCastException}.</p>
 */
public final class NativeRowValues {

    private NativeRowValues() {
    }

    /** Identificador de la columna: acepta {@link UUID} o su texto (con o sin mayusculas); {@code null} se conserva. */
    public static UUID uuid(final Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof UUID identificador) {
            return identificador;
        }
        if (valor instanceof CharSequence texto) {
            return UUID.fromString(texto.toString().strip());
        }
        throw new IllegalArgumentException("La columna no es un identificador UUID: " + valor.getClass().getName());
    }
}
