package co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.Optional;

/**
 * Puerto de entrada para resolver el mensaje de usuario asociado a un codigo.
 *
 * <p>Retorna {@link Optional#empty()} cuando no hay mensaje disponible; nunca propaga fallos del catalogo.</p>
 */
public interface ResolverMensajeUsuarioInputPort extends InteractorWithReturn<String, Optional<String>> {
}
