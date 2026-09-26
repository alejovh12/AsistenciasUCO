package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection;

import java.util.UUID;

/**
 * Fila cruda de la query JPA de asistencia (constructor expression). Interna a Infrastructure:
 * no es la proyeccion del puerto. {@code presente} conserva el nullable de la columna para que el
 * mapper aplique la semantica JDBC ({@code getBoolean}: NULL -> false).
 */
public record AsistenciaQueryRow(
        UUID asistencia,
        UUID estudiante,
        UUID grupo,
        UUID sesion,
        Boolean presente,
        String estado
) {
}
