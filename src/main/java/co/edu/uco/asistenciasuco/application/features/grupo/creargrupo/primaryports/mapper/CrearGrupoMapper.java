package co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.dto.CrearGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.domain.CrearGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.entity.CrearGrupoResultadoEntity;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.dto.CrearGrupoResultadoDTO;

public final class CrearGrupoMapper {

    private CrearGrupoMapper() {
    }

    public static CrearGrupoDomain toDomain(final CrearGrupoDTO dto) {
        return new CrearGrupoDomain(
                dto.idAsignatura(),
                dto.idPeriodoAcademico(),
                dto.codigo(),
                dto.nombre(),
                dto.idDocente(),
                dto.aula(),
                Boolean.TRUE.equals(dto.generarSesionesAutomaticas()),
                dto.usuarioEjecutor()
        );
    }

    public static CrearGrupoResultadoDTO toDTO(final CrearGrupoResultadoEntity entity) {
        return new CrearGrupoResultadoDTO(entity.id(), entity.mensajeUsuario());
    }
}
