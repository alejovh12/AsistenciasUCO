package co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.dto;

import java.util.UUID;

public record GenerarReporteAsistenciaDTO(UUID grupoId, UUID usuarioEjecutor) {
}
