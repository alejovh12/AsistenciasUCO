package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modelo de resultado interno de la consulta de sesiones de la materia del estudiante.
 */
public record SesionMateriaEstudianteDomain(UUID id, String nombre, Integer numero, String codigo, Integer numeroSemana,
                                            UUID idGrupo, String codigoGrupo, String nombreGrupo,
                                            LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin) {
}
