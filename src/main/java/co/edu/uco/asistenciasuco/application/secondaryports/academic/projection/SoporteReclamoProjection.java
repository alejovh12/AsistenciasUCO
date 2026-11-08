package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

/**
 * Soporte vinculado (ATTACHED) a un reclamo, en orden de adjuncion (CULM-001 D.2, HU015). {@code fileId} es el identificador opaco:
 * el archivo se descarga siempre por la API autenticada, nunca por el almacenamiento.
 */
public record SoporteReclamoProjection(UUID fileId, String nombre, String tipoContenido, long tamanioBytes) {
}
