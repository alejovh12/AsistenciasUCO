package co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.primaryports.dto;

import java.util.List;

public record SolicitudesIngresoDocentePaginaDTO(List<SolicitudIngresoDocenteDTO> solicitudes, int totalRegistros) {
}
