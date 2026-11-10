package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * HU019: carga horaria programada de un grupo en el que el estudiante tiene matricula inscrita. Cuenta las sesiones del
 * grupo que no estan canceladas, ya dictadas o por dictar ({@code usp_consultar_horas_programadas_estudiante}).
 */
public record HorasProgramadasEstudianteProjection(UUID idGrupo, int sesionesProgramadas, BigDecimal horasProgramadas) {
}
