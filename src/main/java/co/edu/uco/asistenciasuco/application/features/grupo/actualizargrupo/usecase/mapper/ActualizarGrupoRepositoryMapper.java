package co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.domain.ActualizarGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.entity.ActualizarGrupoResultadoEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoCommandRepositoryProjection;

public final class ActualizarGrupoRepositoryMapper {

    private ActualizarGrupoRepositoryMapper() {
    }

    public static ActualizarGrupoRepositoryDTO toRepositoryDTO(final ActualizarGrupoDomain domain) {
        return new ActualizarGrupoRepositoryDTO(
                domain.idGrupo(),
                domain.codigo(),
                domain.nombre(),
                domain.idDocente(),
                domain.cupoMaximo(),
                domain.aula()
        );
    }

    public static ActualizarGrupoResultadoEntity toEntity(final GrupoCommandRepositoryProjection projection) {
        return new ActualizarGrupoResultadoEntity(projection.idGrupo(), projection.mensajeUsuario());
    }
}
