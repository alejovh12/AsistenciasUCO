package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SesionMateriaEstudianteDTO(UUID id, String nombre, Integer numero, String codigo, Integer numeroSemana,
                                         UUID idGrupo, String codigoGrupo, String nombreGrupo,
                                         LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin) {
}
