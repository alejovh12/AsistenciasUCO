package co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.dto;

import java.util.UUID;

/** Programa academico que se crea junto con el coordinador (DEC-065 (5)). */
public record ProgramaNuevoDTO(String nombre, UUID idTipoPrograma, UUID idModalidad, UUID idJornada) {
}
