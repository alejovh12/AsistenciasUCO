package co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.domain;

import java.util.UUID;

/** Programa academico que se crea en la misma transaccion que el coordinador (DEC-065 (5)). */
public record ProgramaNuevoDomain(String nombre, UUID idTipoPrograma, UUID idModalidad, UUID idJornada) {
}
