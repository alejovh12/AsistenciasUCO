package co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.primaryports.dto;

import java.util.UUID;

/**
 * HU018: asignatura de un semestre del plan. {@code tipoAsignatura} es ESPECIFICA, BASICA o INSTITUCIONAL (el componente
 * de formacion); {@code cursando} indica que el estudiante tiene matricula inscrita en un grupo de la asignatura, y
 * {@code idGrupoCursando} / {@code nombreGrupoCursando} dicen cual (nulos si no la cursa).
 */
public record AsignaturaPlanEstudianteDTO(UUID idAsignatura, String codigo, String nombre, Integer creditos,
                                          String tipoAsignatura, String codigoCaracter, String nombreCaracter,
                                          boolean cursando, UUID idGrupoCursando, String nombreGrupoCursando) {
}
