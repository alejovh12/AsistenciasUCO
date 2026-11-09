package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response;

import java.util.UUID;

/**
 * CULM-001 C.1: resultado de decidir o resolver una revision. {@code mensajeUsuario} es el texto institucional del SP
 * (RCN_007/008/009/010/011/015) listo para mostrar; el cliente recarga la bandeja para ver el estado completo.
 */
public record DecisionRevisionCancelacionResponse(
        UUID id,
        String estadoAnterior,
        String estado,
        String decision,
        boolean sinCambios,
        boolean cambioMatricula,
        String perfilResolutor,
        boolean escalada,
        String mensajeUsuario
) {
}
