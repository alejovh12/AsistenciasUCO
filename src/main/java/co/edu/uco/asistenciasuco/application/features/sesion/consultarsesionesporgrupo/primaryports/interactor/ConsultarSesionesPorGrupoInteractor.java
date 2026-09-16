package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.SesionConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.primaryports.ConsultarSesionesPorGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.primaryports.dto.ConsultarSesionesPorGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.primaryports.mapper.ConsultarSesionesPorGrupoMapper;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.usecase.ConsultarSesionesPorGrupoUseCase;

import java.util.List;
import java.util.Objects;

/**
 * Interactor del puerto de entrada para consultar sesiones por grupo.
 */
public final class ConsultarSesionesPorGrupoInteractor implements ConsultarSesionesPorGrupoInputPort {

    private final ConsultarSesionesPorGrupoUseCase useCase;

    public ConsultarSesionesPorGrupoInteractor(final ConsultarSesionesPorGrupoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso ConsultarSesionesPorGrupoUseCase es obligatorio.");
    }

    @Override
    public List<SesionConsultadaDTO> execute(final ConsultarSesionesPorGrupoDTO dto) {
        return ConsultarSesionesPorGrupoMapper.toDTOs(
                useCase.execute(ConsultarSesionesPorGrupoMapper.toDomain(dto))
        );
    }
}
