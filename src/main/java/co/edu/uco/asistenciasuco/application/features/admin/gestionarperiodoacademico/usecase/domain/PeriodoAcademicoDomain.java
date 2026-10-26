package co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.usecase.domain;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Dominio de la operacion de crear o modificar un periodo academico (HU096/HU097).
 */
public record PeriodoAcademicoDomain(UUID idPeriodo, String nombre, LocalDate fechaInicio, LocalDate fechaFin,
                                     UUID usuario) {
}
