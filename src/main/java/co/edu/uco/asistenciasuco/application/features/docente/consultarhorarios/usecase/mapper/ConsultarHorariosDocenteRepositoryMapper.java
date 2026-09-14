package co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.usecase.domain.HorarioDocenteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorarioDocenteProjection;

public final class ConsultarHorariosDocenteRepositoryMapper {

    private ConsultarHorariosDocenteRepositoryMapper() {
    }

    public static HorarioDocenteDomain toDomain(final HorarioDocenteProjection projection) {
        return new HorarioDocenteDomain(projection.id(), projection.idDocente(), projection.idGrupo(),
                projection.codigoMateria(), projection.nombreMateria(), projection.seccion(), projection.dia(),
                projection.horaInicio(), projection.horaFin(), projection.aula(), projection.totalEstudiantes());
    }
}
