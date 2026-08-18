package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase;

import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.domain.RegistrarEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.entity.RegistrarEstudianteResultadoEntity;
import co.edu.uco.asistenciasuco.application.usecase.UseCaseWithReturn;

/**
 * Caso de uso para registrar un estudiante en un grupo.
 */
public interface RegistrarEstudianteUseCase
        extends UseCaseWithReturn<RegistrarEstudianteDomain, RegistrarEstudianteResultadoEntity> {
}
