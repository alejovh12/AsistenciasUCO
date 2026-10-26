package co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports.dto;

import java.time.LocalDate;
import java.util.UUID;

/** HU096/HU097: datos de entrada para crear o modificar un periodo academico. */
public record GuardarPeriodoAcademicoDTO(UUID idPeriodo, String nombre, LocalDate fechaInicio, LocalDate fechaFin,
                                         UUID usuario) {
}
