package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.AsistenciaQueryRow;

/**
 * Traduce la fila JPA a la proyeccion del puerto con la MISMA semantica que el mapper JDBC:
 * {@code presente} = {@code getBoolean} (NULL -> false), {@code estado} tal cual (sin default ni
 * enum) y {@code observacion} siempre vacia (no es una columna persistida).
 */
public final class AsistenciaJpaProjectionMapper {

    private static final String OBSERVACION_VACIA = "";

    private AsistenciaJpaProjectionMapper() {
    }

    public static AsistenciaRepositoryProjection toProjection(final AsistenciaQueryRow row) {
        return new AsistenciaRepositoryProjection(
                row.asistencia(),
                row.estudiante(),
                row.grupo(),
                row.sesion(),
                Boolean.TRUE.equals(row.presente()),
                row.estado(),
                OBSERVACION_VACIA
        );
    }
}
