package co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.CrearSesionUseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.domain.CrearSesionDomain;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.mapper.CrearSesionRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import java.util.Objects;

/**
 * Implementacion del caso de uso crear sesion.
 */
public final class CrearSesionUseCaseImpl implements CrearSesionUseCase {

    private final SesionRepositoryPort sesionRepositoryPort;
    private final InstitutionalScopePort institutionalScopePort;

    public CrearSesionUseCaseImpl(final SesionRepositoryPort sesionRepositoryPort,
                                  final InstitutionalScopePort institutionalScopePort) {
        this.sesionRepositoryPort = Objects.requireNonNull(sesionRepositoryPort, "El puerto de salida SesionRepositoryPort es obligatorio.");
        this.institutionalScopePort = Objects.requireNonNull(institutionalScopePort, "InstitutionalScopePort es obligatorio.");
    }

    @Override
    public void execute(final CrearSesionDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para crear sesion es obligatorio.");
        }
        final var docenteId = institutionalScopePort.findDocenteIdByUsuario(domain.getDocente())
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el docente autenticado."));
        final var scopedDomain = new CrearSesionDomain(
                domain.getGrupo(), domain.getTema(), domain.getDescripcion(), domain.getFechaHoraInicio(),
                domain.getFechaHoraFin(), domain.getAula(), domain.getTipo(), docenteId
        );
        sesionRepositoryPort.crearSesion(CrearSesionRepositoryMapper.toRepositoryDTO(scopedDomain));
    }
}
