package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.ConsultarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.ConsultarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.SesionConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.mapper.ConsultarSesionMapper;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.ConsultarSesionUseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.domain.ConsultarSesionDomain;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.entity.SesionConsultadaEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Interactor del puerto de entrada para consultar sesion.
 */
public final class ConsultarSesionInteractor implements ConsultarSesionInputPort {

    private final ConsultarSesionUseCase useCase;

    public ConsultarSesionInteractor(final ConsultarSesionUseCase useCase) {
        if (ObjectHelper.isNull(useCase)) {
            throw new CrosscuttingException("El caso de uso ConsultarSesionUseCase es obligatorio.");
        }
        this.useCase = useCase;
    }

    @Override
    public SesionConsultadaDTO execute(final ConsultarSesionDTO dto) {
        final ConsultarSesionDomain domain = ConsultarSesionMapper.toDomain(dto);
        final SesionConsultadaEntity resultado = useCase.execute(domain);
        return ConsultarSesionMapper.toDTO(resultado);
    }
}
