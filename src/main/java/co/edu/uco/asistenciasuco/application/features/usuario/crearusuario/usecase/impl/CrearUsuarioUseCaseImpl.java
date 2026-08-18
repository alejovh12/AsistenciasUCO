package co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.CrearUsuarioUseCase;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.domain.CrearUsuarioDomain;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.entity.CrearUsuarioResultadoEntity;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.mapper.CrearUsuarioRepositoryMapper;
import co.edu.uco.asistenciasuco.application.features.usuario.domain.rules.PasswordRegistroRule;
import co.edu.uco.asistenciasuco.application.secondaryports.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.CrearUsuarioRepositoryEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

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
        if (ObjectHelper.isNull(usuarioRepositoryPort)) {
            throw new CrosscuttingException("El puerto de salida UsuarioRepositoryPort es obligatorio.");
        }
        if (ObjectHelper.isNull(passwordEncoderPort)) {
            throw new CrosscuttingException("El puerto PasswordEncoderPort es obligatorio.");
        }
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    @Override
    public CrearUsuarioResultadoEntity execute(final CrearUsuarioDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para crear usuario es obligatorio.");
        }

        final String encodedPassword = passwordEncoderPort.encode(
                PasswordRegistroRule.resolverCredencialNueva(domain.getPassword(), domain.getNumeroIdentificacion())
        );
        final CrearUsuarioRepositoryEntity resultado = usuarioRepositoryPort.crearUsuario(
                CrearUsuarioRepositoryMapper.toRepositoryDTO(domain, encodedPassword)
        );

        if (ObjectHelper.isNull(resultado)) {
            throw new CrosscuttingException("El resultado de crear usuario es obligatorio.");
        }

        return CrearUsuarioRepositoryMapper.toUseCaseEntity(resultado, domain);
    }
}
