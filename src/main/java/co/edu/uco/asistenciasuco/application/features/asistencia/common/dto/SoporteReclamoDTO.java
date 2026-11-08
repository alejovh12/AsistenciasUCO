package co.edu.uco.asistenciasuco.application.features.asistencia.common.dto;

import java.util.UUID;

/**
 * Soporte de un reclamo (CULM-001 D.2): {@code {fileId, nombre, tipoContenido, tamanioBytes}}. Se descarga SIEMPRE por la API
 * autenticada ({@code GET /api/v1/archivos/{fileId}}), nunca por el almacenamiento.
 */
public record SoporteReclamoDTO(UUID fileId, String nombre, String tipoContenido, long tamanioBytes) {
}
