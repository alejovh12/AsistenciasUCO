package co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.domain.MateriaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.MateriaEstudianteProjection;

public final class ConsultarMateriasEstudianteRepositoryMapper {

    private ConsultarMateriasEstudianteRepositoryMapper() {
    }

    public static MateriaEstudianteDomain toDomain(final MateriaEstudianteProjection projection) {
        return new MateriaEstudianteDomain(projection.idAsignatura(), projection.nombreAsignatura(),
                projection.idGrupo(), projection.nombreGrupo());
    }
}
