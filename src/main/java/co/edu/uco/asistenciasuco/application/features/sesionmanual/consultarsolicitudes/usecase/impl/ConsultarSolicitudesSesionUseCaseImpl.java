package co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudes.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudes.usecase.ConsultarSolicitudesSesionUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.PaginaSolicitudesSesion;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.SolicitudSesion;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

/** DEC-065 (2): consulta paginada de solicitudes; traduce los DATETIME2 en UTC de la DB a instantes. */
public final class ConsultarSolicitudesSesionUseCaseImpl implements ConsultarSolicitudesSesionUseCase {

    static final int PAGINA_POR_DEFECTO = 1;
    static final int TAMANO_POR_DEFECTO = 20;

    private final SesionManualRepositoryPort repositorio;

    public ConsultarSolicitudesSesionUseCaseImpl(final SesionManualRepositoryPort repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "SesionManualRepositoryPort es obligatorio.");
    }

    @Override
    public Resultado execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando para consultar las solicitudes es obligatorio.");
        final int pagina = comando.pagina() == null ? PAGINA_POR_DEFECTO : comando.pagina();
        final int tamano = comando.tamano() == null ? TAMANO_POR_DEFECTO : comando.tamano();
        final PaginaSolicitudesSesion datos = repositorio.consultarSolicitudes(
                comando.usuarioId(), comando.soloPendientes(), comando.programaId(), comando.grupoId(), pagina, tamano);
        return new Resultado(datos.elementos().stream().map(ConsultarSolicitudesSesionUseCaseImpl::aSolicitud).toList(),
                datos.total(), pagina, tamano);
    }

    private static Solicitud aSolicitud(final SolicitudSesion s) {
        return new Solicitud(s.idSesion(), s.nombreSesion(), s.codigoSesion(), s.idGrupo(), s.nombreGrupo(), s.nombreAsignatura(),
                s.idPrograma(), s.nombrePrograma(), s.idDocente(), s.nombreDocente(), instante(s.fechaHoraInicio()),
                instante(s.fechaHoraFin()), s.estadoSolicitud(), s.nombreEstadoSolicitud(), instante(s.fechaSolicitudUtc()),
                instante(s.fechaResolucionUtc()), s.observacion());
    }

    private static Instant instante(final LocalDateTime utc) {
        return utc == null ? null : utc.toInstant(ZoneOffset.UTC);
    }
}
