package co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.usecase.domain;

import java.util.List;

public record SolicitudesIngresoDocentePaginaDomain(List<SolicitudIngresoDocenteDomain> solicitudes, int totalRegistros) {
}
