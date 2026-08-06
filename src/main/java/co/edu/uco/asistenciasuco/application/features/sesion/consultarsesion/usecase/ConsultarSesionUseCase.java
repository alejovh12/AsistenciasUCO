package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase;

import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.domain.ConsultarSesionDomain;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.entity.SesionConsultadaEntity;
import co.edu.uco.asistenciasuco.application.usecase.UseCaseWithReturn;

/**
 * Caso de uso para consultar una sesion.
 */
public interface ConsultarSesionUseCase
        extends UseCaseWithReturn<ConsultarSesionDomain, SesionConsultadaEntity> {
}
