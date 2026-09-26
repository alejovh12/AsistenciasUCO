package co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.usecase;

import co.edu.uco.asistenciasuco.application.usecase.UseCaseWithReturn;

import java.util.Optional;

/**
 * Caso de uso para resolver el mensaje de usuario asociado a un codigo.
 */
public interface ResolverMensajeUsuarioUseCase extends UseCaseWithReturn<String, Optional<String>> {
}
