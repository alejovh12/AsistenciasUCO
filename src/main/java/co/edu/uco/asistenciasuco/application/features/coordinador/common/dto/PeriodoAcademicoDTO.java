package co.edu.uco.asistenciasuco.application.features.coordinador.common.dto;

import java.time.LocalDate;
import java.util.UUID;

public record PeriodoAcademicoDTO(UUID id, UUID idInstitucion, String nombreInstitucion, String nombre,
                                  String codigo, LocalDate fechaInicio, LocalDate fechaFin, Integer anio) {
}
