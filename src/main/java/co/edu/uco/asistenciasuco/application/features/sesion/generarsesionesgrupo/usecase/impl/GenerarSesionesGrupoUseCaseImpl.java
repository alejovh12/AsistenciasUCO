package co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.usecase.GenerarSesionesGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.usecase.domain.GenerarSesionesGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.usecase.mapper.GenerarSesionesGrupoRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.Objects;

public final class GenerarSesionesGrupoUseCaseImpl implements GenerarSesionesGrupoUseCase {

    private final SesionRepositoryPort sesionRepositoryPort;

    public GenerarSesionesGrupoUseCaseImpl(final SesionRepositoryPort sesionRepositoryPort) {
        this.sesionRepositoryPort = Objects.requireNonNull(sesionRepositoryPort, "SesionRepositoryPort es obligatorio.");
    }

    @Override
    public void execute(final GenerarSesionesGrupoDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para generar sesiones de grupo es obligatorio.");
        }
        sesionRepositoryPort.generarSesionesGrupo(GenerarSesionesGrupoRepositoryMapper.toRepositoryDTO(domain));
    }
}
