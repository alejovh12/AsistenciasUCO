package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.request;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.TextoSeguro;
import java.util.UUID;

/** Programa academico que se crea junto con el coordinador (DEC-065 (5)); excluyente con {@code idPrograma}. */
public record ProgramaNuevoRequest(@TextoSeguro String nombre, UUID idTipoPrograma, UUID idModalidad, UUID idJornada) {
}
