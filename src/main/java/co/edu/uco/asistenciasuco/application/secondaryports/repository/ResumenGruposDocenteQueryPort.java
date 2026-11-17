package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * CIERRE-W3I: totales de los grupos habilitados del docente titular ({@code usp_consultar_resumen_grupos_docente}). El ambito
 * (solo sus grupos) y el perfil DOCENTE los valida la base.
 */
public interface ResumenGruposDocenteQueryPort {

    /** Una fila: {@code tasaOcupacion} es {@code null} con cupo total 0. */
    record ResumenGrupos(int gruposHabilitados, int estudiantesActivos, int cupoTotal, BigDecimal tasaOcupacion) {
    }

    ResumenGrupos consultarResumenGruposDocente(UUID usuarioEjecutor);
}
