package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.RegistrarEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.domain.RegistrarEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.mapper.RegistrarEstudianteRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Implementacion del caso de uso registrar estudiante en grupo.
 */
public final class RegistrarEstudianteUseCaseImpl implements RegistrarEstudianteUseCase {

    private final GrupoRepositoryPort grupoRepositoryPort;

    public RegistrarEstudianteUseCaseImpl(final GrupoRepositoryPort grupoRepositoryPort) {
        if (ObjectHelper.isNull(grupoRepositoryPort)) {
            throw new CrosscuttingException("El puerto de salida GrupoRepositoryPort es obligatorio.");
        }
        this.grupoRepositoryPort = grupoRepositoryPort;
    }

    @Override
    public void execute(final RegistrarEstudianteDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para registrar estudiante en grupo es obligatorio.");
        }
        grupoRepositoryPort.registrarEstudianteEnGrupo(RegistrarEstudianteRepositoryMapper.toRepositoryDTO(domain));
    }
}
