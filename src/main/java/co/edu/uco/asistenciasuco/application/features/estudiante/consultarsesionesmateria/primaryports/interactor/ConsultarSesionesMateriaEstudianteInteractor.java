package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.ConsultarSesionesMateriaEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.dto.SesionesMateriaEstudiantePaginaDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.mapper.ConsultarSesionesMateriaEstudianteMapper;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.ConsultarSesionesMateriaEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.SesionMateriaEstudianteDomain;
import java.util.Objects; import java.util.UUID;

public final class ConsultarSesionesMateriaEstudianteInteractor implements ConsultarSesionesMateriaEstudianteInputPort {
    private final ConsultarSesionesMateriaEstudianteUseCase useCase;
    public ConsultarSesionesMateriaEstudianteInteractor(final ConsultarSesionesMateriaEstudianteUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase);
    }
    @Override public SesionesMateriaEstudiantePaginaDTO consultarPagina(final UUID actorUsuarioId, final UUID idAsignatura,
                                                                        final UUID idGrupo, final Integer pagina, final Integer tamano) {
        final PaginaResultado<SesionMateriaEstudianteDomain> resultado =
                useCase.consultarPagina(actorUsuarioId, idAsignatura, idGrupo, pagina, tamano);
        return new SesionesMateriaEstudiantePaginaDTO(
                resultado.elementos().stream().map(ConsultarSesionesMateriaEstudianteMapper::toDTO).toList(),
                resultado.totalElementos());
    }
}
