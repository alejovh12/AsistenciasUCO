package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.usecase.domain;

import java.util.List;

public record SolicitudesIngresoEstudiantePaginaDomain(List<SolicitudIngresoEstudianteDomain> solicitudes, int totalRegistros) {
}
