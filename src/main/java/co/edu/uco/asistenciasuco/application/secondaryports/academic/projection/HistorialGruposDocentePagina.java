package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.List;

/**
 * Pagina del historial de grupos del docente (HU094). El resumen agrega TODO el historial del docente en el programa
 * (no depende del filtro de periodo ni de la pagina); sin filas, el resumen es cero.
 */
public record HistorialGruposDocentePagina(
        List<GrupoHistorialDocenteProjection> grupos,
        long total,
        int totalGrupos,
        int totalPeriodos,
        int gruposVigentes,
        int estudiantesActivosVigentes
) {

    public HistorialGruposDocentePagina {
        grupos = grupos == null ? List.of() : List.copyOf(grupos);
    }
}
