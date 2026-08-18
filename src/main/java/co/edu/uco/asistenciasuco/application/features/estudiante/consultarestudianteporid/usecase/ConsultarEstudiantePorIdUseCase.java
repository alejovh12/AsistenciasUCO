package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.usecase;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.usecase.domain.ConsultarEstudiantePorIdDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.usecase.entity.EstudianteDetalleEntity;
import co.edu.uco.asistenciasuco.application.usecase.UseCaseWithReturn;

public interface ConsultarEstudiantePorIdUseCase
        extends UseCaseWithReturn<ConsultarEstudiantePorIdDomain, EstudianteDetalleEntity> {
}
