package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.domain.ConsultarEstudiantesDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.entity.EstudiantePaginaEntity;
import co.edu.uco.asistenciasuco.application.usecase.UseCaseWithReturn;

public interface ConsultarEstudiantesUseCase
        extends UseCaseWithReturn<ConsultarEstudiantesDomain, EstudiantePaginaEntity> {
}
