package co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase;

import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.domain.CrearUsuarioDomain;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.entity.CrearUsuarioResultadoEntity;
import co.edu.uco.asistenciasuco.application.usecase.UseCaseWithReturn;

/**
 * Caso de uso para crear un usuario base.
 */
public interface CrearUsuarioUseCase extends UseCaseWithReturn<CrearUsuarioDomain, CrearUsuarioResultadoEntity> {
}
