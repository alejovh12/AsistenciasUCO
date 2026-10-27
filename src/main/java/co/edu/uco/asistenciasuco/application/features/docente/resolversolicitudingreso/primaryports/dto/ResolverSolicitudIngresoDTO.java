package co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.primaryports.dto;

import java.util.UUID;

/** Entrada de HU053/HU054: accion ACEPTAR o RECHAZAR; el motivo solo aplica (y es obligatorio) al rechazar. */
public record ResolverSolicitudIngresoDTO(UUID actorUsuarioId, UUID solicitudId, String accion, String motivo) {
}
