package co.edu.uco.asistenciasuco.application.features.docente.common.dto;

import java.math.BigDecimal;

/**
 * CIERRE-W3I: totales de los grupos habilitados del docente titular, calculados en la base. {@code tasaOcupacion} es
 * {@code null} cuando el cupo total es 0 (no se asume un valor).
 */
public record ResumenGruposDocenteDTO(int gruposHabilitados, int estudiantesActivos, int cupoTotal, BigDecimal tasaOcupacion) {
}
