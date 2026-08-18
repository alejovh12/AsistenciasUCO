package co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.usecase;

import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.usecase.domain.RegistrarDocenteDesdeUsuarioDomain;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.usecase.entity.RegistrarDocenteDesdeUsuarioResultadoEntity;
import co.edu.uco.asistenciasuco.application.usecase.UseCaseWithReturn;

public interface RegistrarDocenteDesdeUsuarioUseCase
        extends UseCaseWithReturn<RegistrarDocenteDesdeUsuarioDomain, RegistrarDocenteDesdeUsuarioResultadoEntity> {
}
