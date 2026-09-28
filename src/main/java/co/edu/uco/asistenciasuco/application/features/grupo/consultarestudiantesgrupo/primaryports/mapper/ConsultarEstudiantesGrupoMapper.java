package co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.dto.ConsultarEstudiantesGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.dto.EstudianteGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.domain.ConsultarEstudiantesGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.entity.EstudianteGrupoEntity;

import java.util.List;

public final class ConsultarEstudiantesGrupoMapper {

    private ConsultarEstudiantesGrupoMapper() {
    }

    public static ConsultarEstudiantesGrupoDomain toDomain(final ConsultarEstudiantesGrupoDTO dto) {
        return new ConsultarEstudiantesGrupoDomain(dto.grupoId(), dto.usuarioEjecutor());
    }

    public static List<EstudianteGrupoDTO> toDTOs(final List<EstudianteGrupoEntity> entities) {
        return entities.stream().map(ConsultarEstudiantesGrupoMapper::toDTO).toList();
    }

    private static EstudianteGrupoDTO toDTO(final EstudianteGrupoEntity entity) {
        return new EstudianteGrupoDTO(
                entity.id(),
                entity.idEstudiante(),
                entity.documento(),
                entity.nombreCompleto(),
                entity.correo(),
                entity.codigoEstado(),
                entity.nombreEstado()
        );
    }
}
