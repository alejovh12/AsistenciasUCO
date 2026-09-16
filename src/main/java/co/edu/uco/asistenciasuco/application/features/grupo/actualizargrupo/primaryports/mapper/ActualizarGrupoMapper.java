package co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.dto.ActualizarGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.dto.ActualizarGrupoResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.domain.ActualizarGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.entity.ActualizarGrupoResultadoEntity;

public final class ActualizarGrupoMapper {

    private ActualizarGrupoMapper() {
    }

    public static ActualizarGrupoDomain toDomain(final ActualizarGrupoDTO dto) {
        return new ActualizarGrupoDomain(
                dto.idGrupo(),
                dto.codigo(),
                dto.nombre(),
                dto.idDocente(),
                dto.cupoMaximo(),
                dto.aula(),
                dto.usuarioEjecutor()
        );
    }

    public static ActualizarGrupoResultadoDTO toDTO(final ActualizarGrupoResultadoEntity entity) {
        return new ActualizarGrupoResultadoDTO(entity.id(), entity.mensajeUsuario());
    }
}
