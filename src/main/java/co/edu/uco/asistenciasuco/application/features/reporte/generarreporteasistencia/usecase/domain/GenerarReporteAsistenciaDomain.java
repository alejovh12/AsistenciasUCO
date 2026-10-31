package co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.domain;

import java.util.UUID;

public record GenerarReporteAsistenciaDomain(UUID grupoId, UUID usuarioEjecutor) {
}
