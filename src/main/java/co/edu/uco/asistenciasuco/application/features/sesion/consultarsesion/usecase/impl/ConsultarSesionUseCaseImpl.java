package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.ConsultarSesionUseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.domain.ConsultarSesionDomain;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.entity.SesionConsultadaEntity;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.mapper.ConsultarSesionRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import java.util.Objects;
import java.util.UUID;

/**
 * Implementacion del caso de uso consultar sesion.
 *
 * <p>Autorizacion por pertenencia al grupo de la sesion (la sesion se resuelve primero: 404 si no existe, 403 si
 * existe y el solicitante no tiene relacion con ella). Tiene acceso quien sea docente titular del grupo, estudiante
 * matriculado en el grupo, coordinador activo del programa del grupo o decano activo de la facultad del programa. Una
 * persona con varios roles legitimos accede si alguna de sus identidades cubre el grupo. Sin ninguna relacion
 * institucional no hay acceso por defecto.</p>
 */
public final class ConsultarSesionUseCaseImpl implements ConsultarSesionUseCase {

    private final SesionRepositoryPort sesionRepositoryPort;
    private final InstitutionalScopePort institutionalScopePort;

    public ConsultarSesionUseCaseImpl(
            final SesionRepositoryPort sesionRepositoryPort,
            final InstitutionalScopePort institutionalScopePort
    ) {
        this.sesionRepositoryPort = Objects.requireNonNull(sesionRepositoryPort, "El puerto de salida SesionRepositoryPort es obligatorio.");
        this.institutionalScopePort = Objects.requireNonNull(institutionalScopePort, "InstitutionalScopePort es obligatorio.");
    }

    @Override
    public SesionConsultadaEntity execute(final ConsultarSesionDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para consultar sesion es obligatorio.");
        }

        final SesionRepositoryProjection sesion = sesionRepositoryPort.consultarSesion(
                ConsultarSesionRepositoryMapper.toRepositoryDTO(domain)
        );
        if (sesion == null) {
            throw new ResourceNotFoundException(SesionErrorCode.ERR_SESION_NO_EXISTE);
        }
        if (!puedeConsultar(domain.getUsuarioEjecutor(), sesion.getGrupo())) {
            throw new ForbiddenException("El usuario autenticado no tiene relacion institucional con la sesion consultada.");
        }
        return ConsultarSesionRepositoryMapper.toUseCaseEntity(sesion);
    }

    private boolean puedeConsultar(final UUID usuario, final UUID grupo) {
        return institutionalScopePort.canDocenteAccessGrupo(usuario, grupo)
                || institutionalScopePort.canEstudianteAccessGrupo(usuario, grupo)
                || institutionalScopePort.canCoordinadorAccessGrupo(usuario, grupo)
                || institutionalScopePort.canDecanoAccessGrupo(usuario, grupo);
    }
}
