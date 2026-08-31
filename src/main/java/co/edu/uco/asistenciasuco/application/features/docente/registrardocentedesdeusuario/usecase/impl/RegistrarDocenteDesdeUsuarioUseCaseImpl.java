package co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.usecase.RegistrarDocenteDesdeUsuarioUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.usecase.domain.RegistrarDocenteDesdeUsuarioDomain;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.usecase.entity.RegistrarDocenteDesdeUsuarioResultadoEntity;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.usecase.mapper.RegistrarDocenteDesdeUsuarioRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DocenteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.DocenteOperacionRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import java.util.Objects;

/**
 * Implementacion del caso de uso registrar docente desde usuario.
 */
public final class RegistrarDocenteDesdeUsuarioUseCaseImpl implements RegistrarDocenteDesdeUsuarioUseCase {

    private final DocenteRepositoryPort docenteRepositoryPort;

    public RegistrarDocenteDesdeUsuarioUseCaseImpl(final DocenteRepositoryPort docenteRepositoryPort) {
        this.docenteRepositoryPort = Objects.requireNonNull(docenteRepositoryPort, "El puerto de salida DocenteRepositoryPort es obligatorio.");
    }

    @Override
    public RegistrarDocenteDesdeUsuarioResultadoEntity execute(final RegistrarDocenteDesdeUsuarioDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para registrar docente desde usuario es obligatorio.");
        }

        final DocenteOperacionRepositoryProjection resultado = docenteRepositoryPort.registrarDocenteDesdeUsuario(
                RegistrarDocenteDesdeUsuarioRepositoryMapper.toRepositoryDTO(domain)
        );

        if (ObjectHelper.isNull(resultado)) {
            throw new CrosscuttingException("El resultado de registrar docente desde usuario es obligatorio.");
        }

        return RegistrarDocenteDesdeUsuarioRepositoryMapper.toUseCaseEntity(resultado, domain);
    }
}