package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.GuardarMotivoCancelacionCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.MotivoCancelacionAdminProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoCatalogoAdminProjection;

import java.util.List;
import java.util.UUID;

/** Administracion (solo ADMINISTRADOR, forzado por los SP) del catalogo de motivos de cancelacion de sesion. */
public interface MotivoCancelacionAdminPort {

    /** Todos los registros, activos e inactivos. */
    List<MotivoCancelacionAdminProjection> consultarTodos();

    ResultadoCatalogoAdminProjection crear(GuardarMotivoCancelacionCommand command);

    ResultadoCatalogoAdminProjection actualizar(GuardarMotivoCancelacionCommand command);

    ResultadoCatalogoAdminProjection cambiarEstado(UUID id, boolean estaActivo, UUID usuarioEjecutor);
}
