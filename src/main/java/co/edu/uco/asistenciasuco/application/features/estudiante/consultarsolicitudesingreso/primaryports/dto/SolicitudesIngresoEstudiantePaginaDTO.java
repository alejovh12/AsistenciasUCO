package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.primaryports.dto;

import java.util.List;

public record SolicitudesIngresoEstudiantePaginaDTO(List<SolicitudIngresoEstudianteDTO> solicitudes, int totalRegistros) {
}
