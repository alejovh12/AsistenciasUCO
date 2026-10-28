package co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.usecase;

import co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.usecase.domain.ResolverSolicitudIngresoDomain;

public interface ResolverSolicitudIngresoUseCase {

    /** @return estado legible resultante: ACEPTADA, EN_ESPERA o RECHAZADA. */
    String execute(ResolverSolicitudIngresoDomain domain);
}
