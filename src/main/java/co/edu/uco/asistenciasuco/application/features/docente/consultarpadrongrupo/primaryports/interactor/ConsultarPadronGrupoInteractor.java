package co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.primaryports.ConsultarPadronGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.primaryports.dto.ConsultarPadronGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.primaryports.dto.PadronGrupoPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.primaryports.mapper.ConsultarPadronGrupoMapper;
import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.usecase.ConsultarPadronGrupoUseCase;

import java.util.Objects;

public final class ConsultarPadronGrupoInteractor implements ConsultarPadronGrupoInputPort {

    private final ConsultarPadronGrupoUseCase useCase;

    public ConsultarPadronGrupoInteractor(final ConsultarPadronGrupoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public PadronGrupoPaginaDTO execute(final ConsultarPadronGrupoDTO dto) {
        return ConsultarPadronGrupoMapper.toDTO(useCase.execute(ConsultarPadronGrupoMapper.toDomain(dto)));
    }
}
