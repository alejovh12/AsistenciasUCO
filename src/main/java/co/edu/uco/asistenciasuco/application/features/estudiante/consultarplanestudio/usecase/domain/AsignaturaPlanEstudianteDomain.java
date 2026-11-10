package co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.usecase.domain;

import java.util.UUID;

/** HU018: asignatura de un semestre del plan del estudiante. */
public record AsignaturaPlanEstudianteDomain(UUID idAsignatura, String codigo, String nombre, Integer creditos,
                                             String tipoAsignatura, String codigoCaracter, String nombreCaracter,
                                             boolean cursando, UUID idGrupoCursando, String nombreGrupoCursando) {
}
