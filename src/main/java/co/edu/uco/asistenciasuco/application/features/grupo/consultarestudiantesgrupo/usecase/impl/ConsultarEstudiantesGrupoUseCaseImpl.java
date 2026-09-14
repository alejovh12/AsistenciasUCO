package co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.ConsultarEstudiantesGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.domain.ConsultarEstudiantesGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.entity.EstudianteGrupoEntity;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.mapper.ConsultarEstudiantesGrupoRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;

import java.util.List;
import java.util.Objects;

public final class ConsultarEstudiantesGrupoUseCaseImpl implements ConsultarEstudiantesGrupoUseCase {

    private final GrupoRepositoryPort grupoRepositoryPort;

    public ConsultarEstudiantesGrupoUseCaseImpl(final GrupoRepositoryPort grupoRepositoryPort) {
        this.grupoRepositoryPort = Objects.requireNonNull(grupoRepositoryPort, "El puerto GrupoRepositoryPort es obligatorio.");
    }

    @Override
    public List<EstudianteGrupoEntity> execute(final ConsultarEstudiantesGrupoDomain domain) {
        return ConsultarEstudiantesGrupoRepositoryMapper.toEntities(
                grupoRepositoryPort.consultarEstudiantesGrupo(domain.grupoId())
        );
    }
}
