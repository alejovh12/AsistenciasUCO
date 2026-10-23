package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure;

import java.util.List;

/**
 * Fila de un procedimiento almacenado publico: la salida canonica mas las columnas adicionales que declara su
 * contrato (p. ej. {@code idUsuario}). Vive solo en Infrastructure. Las columnas extra pueden ser nulas cuando el
 * SP rechaza la operacion.
 *
 * @param result resultado canonico (correlacion, mensajes y estado)
 * @param extras columnas posteriores a las 4 canonicas, en el orden del contrato del SP
 */
public record ProcedureRow(ProcedureResult result, List<Object> extras) {
}
