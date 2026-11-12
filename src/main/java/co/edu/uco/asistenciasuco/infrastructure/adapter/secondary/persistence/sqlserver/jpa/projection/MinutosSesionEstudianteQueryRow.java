package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection;

import java.util.UUID;

/** Fila JPQL de los minutos por sesion de {@code uv_asistencia_sesion_horas}. */
public record MinutosSesionEstudianteQueryRow(UUID idSesion, Integer minutosSesion, Integer minutosAsistidos, Integer minutosFalta,
                                              Integer minutosExcusa, Integer minutosSinRegistro, String codigoEstadoSesion) {
}
