package co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports.dto;

import java.util.List;

public record PaginaPeriodosAdminDTO(List<PeriodoAdminDTO> elementos, long total) {
}
