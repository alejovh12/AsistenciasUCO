package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection;

import java.time.LocalDateTime;
import java.util.UUID;

/** Fila JPQL de {@code uv_asistencia_toma_estudiante}; la fecha es DATETIME2 en UTC sin zona. */
public record AsistenciaTomaEstudianteQueryRow(UUID idSesion, UUID idToma, Integer numeroToma, Integer ordenPuntoControl,
                                               LocalDateTime fechaPuntoControlUtc, String codigoEstadoToma,
                                               Boolean esReclamable, UUID idSolicitud, String codigoEstadoSolicitud,
                                               String codigoResultadoAprobacion) {
}
