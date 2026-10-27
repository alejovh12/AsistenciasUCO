package co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.usecase;

import co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.usecase.domain.SolicitudesIngresoDocentePaginaDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.usecase.domain.ConsultarSolicitudesIngresoDocenteDomain;

public interface ConsultarSolicitudesIngresoDocenteUseCase {

    SolicitudesIngresoDocentePaginaDomain execute(ConsultarSolicitudesIngresoDocenteDomain domain);
}
