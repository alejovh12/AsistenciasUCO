package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

/** Vinculos activos e inactivos de un tipo de vinculacion en el programa del coordinador (HU095). */
public record ResumenTipoVinculacionProjection(
        UUID idTipoVinculacion,
        String codigoTipoVinculacion,
        String nombreTipoVinculacion,
        int totalActivos,
        int totalInactivos
) {
}
