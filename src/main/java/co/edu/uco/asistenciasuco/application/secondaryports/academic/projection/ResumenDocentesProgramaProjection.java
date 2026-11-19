package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

/**
 * HU095: vinculos activos e inactivos del programa del coordinador (una fila de usp_consultar_resumen_docentes_programa).
 * MEJORAS-SERGIO-2026-11-18: ya no se agrupa por tipo de vinculacion (retirado del sistema).
 */
public record ResumenDocentesProgramaProjection(int totalActivos, int totalInactivos) {

    public static ResumenDocentesProgramaProjection vacio() {
        return new ResumenDocentesProgramaProjection(0, 0);
    }
}
