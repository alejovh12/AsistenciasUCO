package co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.domain.CrearGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.entity.CrearGrupoResultadoEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoCommandRepositoryProjection;

import java.util.UUID;

public final class CrearGrupoRepositoryMapper {

    private CrearGrupoRepositoryMapper() {
    }

    public static CrearGrupoRepositoryDTO toRepositoryDTO(final CrearGrupoDomain domain, final UUID idGrupo) {
        return new CrearGrupoRepositoryDTO(
                idGrupo,
                domain.idAsignatura(),
                domain.idPeriodoAcademico(),
                domain.codigo(),
                domain.nombre(),
                domain.idDocente(),
                domain.usuarioEjecutor()
        );
    }

    public static CrearGrupoResultadoEntity toEntity(final GrupoCommandRepositoryProjection projection) {
        return new CrearGrupoResultadoEntity(projection.idGrupo(), projection.mensajeUsuario());
    }
}
