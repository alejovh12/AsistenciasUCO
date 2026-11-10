package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.response;

import java.util.UUID;

/** Respuesta de {@code POST /api/v1/decano/coordinadores} (DEC-065 (5)): el coordinador y el programa vinculado o creado. */
public record CrearCoordinadorResponse(UUID idCoordinador, UUID idPrograma, String nombrePrograma) {
}
