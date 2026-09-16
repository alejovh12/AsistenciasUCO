package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.RegistrarAsistenciasSesionUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.domain.RegistrarAsistenciasSesionDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.mapper.RegistrarAsistenciasSesionRepositoryMapper;
import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Implementacion del caso de uso registrar asistencias de sesion (command canonico por lote).
 *
 * <p>Orden obligatorio: (1) resolver el grupo de la sesion, (2) validar titularidad del docente
 * autenticado sobre ese grupo via {@link InstitutionalScopePort} (autorizacion preventiva en
 * Application; la DB repite la misma validacion como defensa en profundidad), (3) persistir el
 * lote a traves de {@link AsistenciaRepositoryPort} (que internamente hace COMMIT en SQL Server),
 * y solo despues de una persistencia exitosa (4) publicar el evento de negocio
 * {@code ASISTENCIAS_SESION_ACTUALIZADAS} via {@link RealtimePublisherPort}. Si cualquier paso
 * previo falla, no se publica evento alguno.</p>
 */
public final class RegistrarAsistenciasSesionUseCaseImpl implements RegistrarAsistenciasSesionUseCase {

    static final String EVENT_TYPE_ASISTENCIAS_SESION_ACTUALIZADAS = "ASISTENCIAS_SESION_ACTUALIZADAS";

    private final AsistenciaRepositoryPort asistenciaRepositoryPort;
    private final SesionRepositoryPort sesionRepositoryPort;
    private final InstitutionalScopePort institutionalScopePort;
    private final RealtimePublisherPort realtimePublisherPort;

    public RegistrarAsistenciasSesionUseCaseImpl(
            final AsistenciaRepositoryPort asistenciaRepositoryPort,
            final SesionRepositoryPort sesionRepositoryPort,
            final InstitutionalScopePort institutionalScopePort,
            final RealtimePublisherPort realtimePublisherPort
    ) {
        this.asistenciaRepositoryPort = Objects.requireNonNull(asistenciaRepositoryPort, "AsistenciaRepositoryPort es obligatorio.");
        this.sesionRepositoryPort = Objects.requireNonNull(sesionRepositoryPort, "SesionRepositoryPort es obligatorio.");
        this.institutionalScopePort = Objects.requireNonNull(institutionalScopePort, "InstitutionalScopePort es obligatorio.");
        this.realtimePublisherPort = Objects.requireNonNull(realtimePublisherPort, "RealtimePublisherPort es obligatorio.");
    }

    @Override
    public void execute(final RegistrarAsistenciasSesionDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para registrar asistencias de sesion es obligatorio.");
        }

        final UUID grupoId = resolveGrupoDeLaSesion(domain.getSesion());
        autorizarTitularidadDocente(domain.getUsuarioEjecutor(), grupoId);

        asistenciaRepositoryPort.registrarAsistenciasSesion(
                RegistrarAsistenciasSesionRepositoryMapper.toRepositoryDTO(domain)
        );

        publicarEventoRealtime(grupoId, domain);
    }

    private UUID resolveGrupoDeLaSesion(final UUID sesionId) {
        final SesionRepositoryProjection sesion = sesionRepositoryPort.consultarSesion(
                new ConsultarSesionRepositoryDTO(sesionId)
        );
        if (ObjectHelper.isNull(sesion)) {
            throw new ResourceNotFoundException(SesionErrorCode.ERR_SESION_NO_EXISTE);
        }
        return sesion.getGrupo();
    }

    private void autorizarTitularidadDocente(final UUID usuarioEjecutor, final UUID grupoId) {
        if (!institutionalScopePort.canDocenteAccessGrupo(usuarioEjecutor, grupoId)) {
            throw new ForbiddenException(AsistenciaErrorCode.ERR_DOCENTE_SIN_TITULARIDAD_SESION);
        }
    }

    private void publicarEventoRealtime(final UUID grupoId, final RegistrarAsistenciasSesionDomain domain) {
        realtimePublisherPort.publish(RealtimeEvent.of(EVENT_TYPE_ASISTENCIAS_SESION_ACTUALIZADAS, Map.of(
                "grupo", grupoId.toString(),
                "sesion", domain.getSesion().toString(),
                "totalRegistros", domain.getRegistros().size()
        )));
    }
}
