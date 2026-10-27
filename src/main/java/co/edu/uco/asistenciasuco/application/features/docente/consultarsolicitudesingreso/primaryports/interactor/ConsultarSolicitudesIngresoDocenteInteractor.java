package co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.primaryports.ConsultarSolicitudesIngresoDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.primaryports.dto.ConsultarSolicitudesIngresoDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.primaryports.dto.SolicitudIngresoDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.primaryports.dto.SolicitudesIngresoDocentePaginaDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.usecase.ConsultarSolicitudesIngresoDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.usecase.domain.ConsultarSolicitudesIngresoDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.usecase.domain.SolicitudesIngresoDocentePaginaDomain;

import java.util.Objects;

public final class ConsultarSolicitudesIngresoDocenteInteractor implements ConsultarSolicitudesIngresoDocenteInputPort {

    private final ConsultarSolicitudesIngresoDocenteUseCase useCase;

    public ConsultarSolicitudesIngresoDocenteInteractor(final ConsultarSolicitudesIngresoDocenteUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public SolicitudesIngresoDocentePaginaDTO execute(final ConsultarSolicitudesIngresoDocenteDTO dto) {
        final SolicitudesIngresoDocentePaginaDomain pagina = useCase.execute(ConsultarSolicitudesIngresoDocenteDomain.of(
                dto == null ? null : dto.actorUsuarioId(),
                dto == null ? null : dto.grupoId(),
                dto == null ? null : dto.estado(),
                dto == null ? null : dto.pagina(),
                dto == null ? null : dto.tamano()));
        return new SolicitudesIngresoDocentePaginaDTO(pagina.solicitudes().stream().map(s -> new SolicitudIngresoDocenteDTO(
                s.id(), s.estudianteId(), s.estudianteNombre(), s.numeroIdentificacion(), s.estado(), s.fechaSolicitudUtc(),
                s.fechaResolucionUtc(), s.motivoRechazo(), s.posicionEspera(), s.capacidadGrupo(), s.cuposDisponibles()))
                .toList(), pagina.totalRegistros());
    }
}
