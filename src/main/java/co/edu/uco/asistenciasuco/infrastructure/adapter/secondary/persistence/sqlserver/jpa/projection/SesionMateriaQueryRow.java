package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection;

import java.util.Date;
import java.util.UUID;

/**
 * Fila JPQL de sesiones de materia del estudiante sobre {@code uv_sesion} (LB-008 JPA-05).
 * Las fechas se convierten con la conversion UTC certificada en JPA-04, no con el mapeo JDBC retirado de produccion.
 */
public record SesionMateriaQueryRow(UUID id, String nombre, Integer numero, String codigo, Integer numeroSemana,
                                    UUID idGrupo, Integer codigoGrupo, String nombreGrupo, Date fechaHoraInicio,
                                    Date fechaHoraFin) {
}
