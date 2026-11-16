package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.PadronGrupoPaginaProjection;

import java.util.UUID;

/**
 * Padron paginado de un grupo (CIERRE-W1A). Paginacion, busqueda y ambito (docente titular) los resuelve el SP
 * {@code usp_consultar_estudiantes_grupo_ambito_paginado}; un grupo ajeno o inexistente es ForbiddenException (403, SEC_002).
 */
public interface PadronGrupoQueryPort {

    PadronGrupoPaginaProjection consultarPadronPagina(
            UUID grupoId,
            UUID usuarioEjecutor,
            int numeroPagina,
            int tamanoPagina,
            String busqueda
    );
}
