package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.primaryports.ConsultarSolicitudesIngresoEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.primaryports.dto.ConsultarSolicitudesIngresoEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.primaryports.dto.SolicitudIngresoEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.primaryports.dto.SolicitudesIngresoEstudiantePaginaDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.usecase.ConsultarSolicitudesIngresoEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.usecase.domain.ConsultarSolicitudesIngresoEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.usecase.domain.SolicitudesIngresoEstudiantePaginaDomain;

import java.util.Objects;

public final class ConsultarSolicitudesIngresoEstudianteInteractor implements ConsultarSolicitudesIngresoEstudianteInputPort {

    private final ConsultarSolicitudesIngresoEstudianteUseCase useCase;

    public ConsultarSolicitudesIngresoEstudianteInteractor(final ConsultarSolicitudesIngresoEstudianteUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public SolicitudesIngresoEstudiantePaginaDTO execute(final ConsultarSolicitudesIngresoEstudianteDTO dto) {
        final SolicitudesIngresoEstudiantePaginaDomain pagina = useCase.execute(ConsultarSolicitudesIngresoEstudianteDomain.of(
                dto == null ? null : dto.actorUsuarioId(),
                dto == null ? null : dto.estado(),
                dto == null ? null : dto.pagina(),
                dto == null ? null : dto.tamano()));
        return new SolicitudesIngresoEstudiantePaginaDTO(pagina.solicitudes().stream().map(s -> new SolicitudIngresoEstudianteDTO(
                s.id(), s.grupoId(), s.grupoCodigo(), s.grupoNombre(), s.asignaturaId(), s.asignaturaNombre(),
                s.docenteNombre(), s.estado(), s.fechaSolicitudUtc(), s.fechaResolucionUtc(), s.motivoRechazo()))
                .toList(), pagina.totalRegistros());
    }
}
