package co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.CrearUsuarioUseCase;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.domain.CrearUsuarioDomain;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.entity.CrearUsuarioResultadoEntity;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.mapper.CrearUsuarioRepositoryMapper;
import co.edu.uco.asistenciasuco.application.features.usuario.domain.rules.PasswordRegistroRule;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.CrearUsuarioRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import java.util.Objects;

/**
 * Implementacion del caso de uso crear usuario.
 */
public final class CrearUsuarioUseCaseImpl implements CrearUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;

    public CrearUsuarioUseCaseImpl(
            final UsuarioRepositoryPort usuarioRepositoryPort,
            final PasswordEncoderPort passwordEncoderPort
    ) {
        this.usuarioRepositoryPort = Objects.requireNonNull(usuarioRepositoryPort, "El puerto de salida UsuarioRepositoryPort es obligatorio.");
        this.passwordEncoderPort = Objects.requireNonNull(passwordEncoderPort, "El puerto PasswordEncoderPort es obligatorio.");
    }

    @Override
    public CrearUsuarioResultadoEntity execute(final CrearUsuarioDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para crear usuario es obligatorio.");
        }

        final String encodedPassword = passwordEncoderPort.encode(
                PasswordRegistroRule.resolverCredencialNueva(domain.getPassword(), domain.getNumeroIdentificacion())
        );
        final CrearUsuarioRepositoryProjection resultado = usuarioRepositoryPort.crearUsuario(
                CrearUsuarioRepositoryMapper.toRepositoryDTO(domain, encodedPassword)
        );

        if (ObjectHelper.isNull(resultado)) {
            throw new CrosscuttingException("El resultado de crear usuario es obligatorio.");
        }

        return CrearUsuarioRepositoryMapper.toUseCaseEntity(resultado, domain);
    }
}