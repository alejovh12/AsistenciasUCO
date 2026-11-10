package co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports;

import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.dto.CrearSesionDTO;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

/**
 * Puerto de entrada para crear una sesion. Devuelve el {@code mensajeUsuario} del SP (DEC-065 (2): {@code SES_022} si la sesion
 * del docente queda como solicitud pendiente de aprobacion).
 */
public interface CrearSesionInputPort extends InteractorWithReturn<CrearSesionDTO, String> {
}
