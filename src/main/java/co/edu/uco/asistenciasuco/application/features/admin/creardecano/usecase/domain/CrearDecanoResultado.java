package co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.domain;

import java.util.UUID;

/** Resultado del alta del decano; facultad nula cuando el decano queda sin facultad. */
public record CrearDecanoResultado(UUID idDecano, UUID idFacultad, String nombreFacultad) {
}
