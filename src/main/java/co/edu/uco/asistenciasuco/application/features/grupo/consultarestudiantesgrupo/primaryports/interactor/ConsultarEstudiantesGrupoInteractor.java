package co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.ConsultarEstudiantesGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.dto.ConsultarEstudiantesGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.dto.EstudianteGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.mapper.ConsultarEstudiantesGrupoMapper;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.ConsultarEstudiantesGrupoUseCase;

import java.util.List;
import java.util.Objects;

public final class ConsultarEstudiantesGrupoInteractor implements ConsultarEstudiantesGrupoInputPort {

    private final ConsultarEstudiantesGrupoUseCase useCase;

    public ConsultarEstudiantesGrupoInteractor(final ConsultarEstudiantesGrupoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso ConsultarEstudiantesGrupoUseCase es obligatorio.");
    }

    @Override
    public List<EstudianteGrupoDTO> execute(final ConsultarEstudiantesGrupoDTO dto) {
        return ConsultarEstudiantesGrupoMapper.toDTOs(useCase.execute(ConsultarEstudiantesGrupoMapper.toDomain(dto)));
    }
}
