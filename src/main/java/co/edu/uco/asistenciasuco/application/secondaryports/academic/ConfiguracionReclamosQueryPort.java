package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ConfiguracionReclamosProjection;

import java.util.UUID;

/**
 * Consulta de la configuracion institucional del formulario de reclamos (CULM-001 D.2, HU015). La base de datos valida que el
 * ejecutor sea ESTUDIANTE o DOCENTE ({@code usp_consultar_configuracion_reclamos}).
 */
public interface ConfiguracionReclamosQueryPort {

    /** @return la fila de configuracion, o {@code null} si el procedimiento no devolvio ninguna */
    ConfiguracionReclamosProjection consultar(UUID usuarioEjecutor);
}
