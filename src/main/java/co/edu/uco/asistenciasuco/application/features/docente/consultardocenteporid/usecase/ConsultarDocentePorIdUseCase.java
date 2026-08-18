package co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.usecase;

import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.entity.DocenteIdentidadEntity;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.usecase.domain.ConsultarDocentePorIdDomain;
import co.edu.uco.asistenciasuco.application.usecase.UseCaseWithReturn;

public interface ConsultarDocentePorIdUseCase
        extends UseCaseWithReturn<ConsultarDocentePorIdDomain, DocenteIdentidadEntity> {
}
