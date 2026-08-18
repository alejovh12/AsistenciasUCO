package co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase;

import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase.domain.ConsultarAsignacionesAcademicasDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase.entity.DocenteAsignacionAcademicaEntity;
import co.edu.uco.asistenciasuco.application.usecase.UseCaseWithReturn;

import java.util.List;

public interface ConsultarAsignacionesAcademicasDocenteUseCase
        extends UseCaseWithReturn<ConsultarAsignacionesAcademicasDocenteDomain, List<DocenteAsignacionAcademicaEntity>> {
}
