package co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase;

import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.domain.CrearSesionDomain;
import co.edu.uco.asistenciasuco.application.usecase.UseCaseWithReturn;

/**
 * Caso de uso para crear una sesion. Devuelve el {@code mensajeUsuario} que entrego el SP.
 */
public interface CrearSesionUseCase extends UseCaseWithReturn<CrearSesionDomain, String> {
}
