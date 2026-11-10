package co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.dto;

import java.util.UUID;

/** Resultado del alta del decano: su identificador y la facultad vinculada o creada (nulos si quedo sin facultad). */
public record CrearDecanoResultadoDTO(UUID idDecano, UUID idFacultad, String nombreFacultad) {
}
