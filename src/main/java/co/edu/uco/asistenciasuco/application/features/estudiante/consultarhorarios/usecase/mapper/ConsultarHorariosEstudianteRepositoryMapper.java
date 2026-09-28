package co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.usecase.domain.HorarioEstudianteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorarioEstudianteProjection;

public final class ConsultarHorariosEstudianteRepositoryMapper {

    private ConsultarHorariosEstudianteRepositoryMapper() {
    }

    public static HorarioEstudianteDomain toDomain(final HorarioEstudianteProjection projection) {
        return new HorarioEstudianteDomain(projection.id(), projection.idEstudiante(), projection.idGrupo(),
                projection.codigoMateria(), projection.nombreMateria(), projection.grupo(), projection.dia(),
                projection.horaInicio(), projection.horaFin(), projection.docente());
    }
}
