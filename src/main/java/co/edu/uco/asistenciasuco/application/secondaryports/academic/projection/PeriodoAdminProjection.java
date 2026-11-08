package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.time.LocalDate;
import java.util.UUID;

/** Periodo academico de la institucion del administrador con su vigencia (CULM-001 D.9-a, F-ADM-030). */
public record PeriodoAdminProjection(UUID id, UUID idInstitucion, String nombreInstitucion, String nombre, String codigo,
                                     LocalDate fechaInicio, LocalDate fechaFin, Integer anio, boolean esVigente) {
}
