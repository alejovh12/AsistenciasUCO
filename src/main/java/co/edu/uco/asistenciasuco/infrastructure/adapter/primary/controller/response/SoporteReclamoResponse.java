package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response;

import java.util.UUID;

/**
 * Soporte de un reclamo (CULM-001 D.2): {@code {fileId, nombre, tipoContenido, tamanioBytes}}. Se descarga por
 * {@code GET /api/v1/archivos/{fileId}} autenticado, nunca por el almacenamiento.
 */
public record SoporteReclamoResponse(UUID fileId, String nombre, String tipoContenido, long tamanioBytes) {
}
