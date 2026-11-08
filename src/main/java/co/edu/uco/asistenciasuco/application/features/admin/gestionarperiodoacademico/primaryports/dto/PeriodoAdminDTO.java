package co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports.dto;

import java.time.LocalDate;
import java.util.UUID;

/** Periodo academico con su vigencia, tal como lo ve el administrador (CULM-001 D.9-a, F-ADM-030). */
public record PeriodoAdminDTO(UUID id, UUID idInstitucion, String nombreInstitucion, String nombre, String codigo,
                              LocalDate fechaInicio, LocalDate fechaFin, Integer anio, boolean esVigente) {
}
