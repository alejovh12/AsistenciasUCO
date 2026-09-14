package co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.domain.EstudianteProgramaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.EstudianteProgramaProjection;

public final class ConsultarEstudiantesProgramaRepositoryMapper {

    private ConsultarEstudiantesProgramaRepositoryMapper() {
    }

    public static EstudianteProgramaDomain toDomain(final EstudianteProgramaProjection projection) {
        return new EstudianteProgramaDomain(
                projection.id(),
                projection.idUsuario(),
                projection.numeroIdentificacion(),
                projection.nombreCompleto(),
                projection.correo(),
                projection.idPrograma(),
                projection.nombrePrograma()
        );
    }
}
