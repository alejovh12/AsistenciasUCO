package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.GuardarCategoriaJustificacionCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CategoriaJustificacionAdminProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoCatalogoAdminProjection;

import java.util.List;
import java.util.UUID;

/** Administracion (solo ADMINISTRADOR, forzado por los SP) del catalogo de categorias de justificacion. */
public interface CategoriaJustificacionAdminPort {

    /** Todos los registros, activos e inactivos. */
    List<CategoriaJustificacionAdminProjection> consultarTodos();

    ResultadoCatalogoAdminProjection crear(GuardarCategoriaJustificacionCommand command);

    ResultadoCatalogoAdminProjection actualizar(GuardarCategoriaJustificacionCommand command);

    ResultadoCatalogoAdminProjection cambiarEstado(UUID id, boolean estaActiva, UUID usuarioEjecutor);
}
