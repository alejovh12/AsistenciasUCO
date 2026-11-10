package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response;

import java.util.UUID;

/** Respuesta de {@code POST /api/v1/admin/decanos} (DEC-065 (5)): el decano y la facultad vinculada o creada (nulos si no tiene). */
public record CrearDecanoResponse(UUID idDecano, UUID idFacultad, String nombreFacultad) {
}
