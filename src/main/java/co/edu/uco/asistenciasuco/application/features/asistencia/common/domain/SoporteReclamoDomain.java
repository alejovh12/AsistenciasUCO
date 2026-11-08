package co.edu.uco.asistenciasuco.application.features.asistencia.common.domain;

import java.util.UUID;

/** Soporte de un reclamo en el modelo interno de los casos de uso (CULM-001 D.2). */
public record SoporteReclamoDomain(UUID fileId, String nombre, String tipoContenido, long tamanioBytes) {
}
