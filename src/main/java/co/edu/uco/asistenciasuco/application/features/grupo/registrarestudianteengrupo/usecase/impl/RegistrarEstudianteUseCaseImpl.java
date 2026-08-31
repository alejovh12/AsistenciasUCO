package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.impl;


import co.edu.uco.asistenciasuco.application.features.estudiante.exception.EstudianteErrorCode;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.RegistrarEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.domain.RegistrarEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.entity.RegistrarEstudianteResultadoEntity;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.mapper.RegistrarEstudianteRepositoryMapper;
import co.edu.uco.asistenciasuco.application.features.usuario.domain.rules.PasswordRegistroRule;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.RegistrarEstudianteRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.Optional;
import java.util.Objects;

/**
 * Implementacion del caso de uso registrar estudiante en grupo.
 */
public final class RegistrarEstudianteUseCaseImpl implements RegistrarEstudianteUseCase {

    private final GrupoRepositoryPort grupoRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;

    public RegistrarEstudianteUseCaseImpl(
            final GrupoRepositoryPort grupoRepositoryPort,
            final UsuarioRepositoryPort usuarioRepositoryPort,
            final PasswordEncoderPort passwordEncoderPort
    ) {
        this.grupoRepositoryPort = Objects.requireNonNull(grupoRepositoryPort, "El puerto de salida GrupoRepositoryPort es obligatorio.");
        this.usuarioRepositoryPort = Objects.requireNonNull(usuarioRepositoryPort, "El puerto de salida UsuarioRepositoryPort es obligatorio.");
        this.passwordEncoderPort = Objects.requireNonNull(passwordEncoderPort, "El puerto PasswordEncoderPort es obligatorio.");
    }

    @Override
    public RegistrarEstudianteResultadoEntity execute(final RegistrarEstudianteDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para registrar estudiante en grupo es obligatorio.");
        }
        final boolean usuarioExistente = validarConflictoIdentidadUsuario(domain);
        final String passwordParaPersistir = resolverPasswordParaPersistir(domain, usuarioExistente);
        final RegistrarEstudianteRepositoryProjection resultado = grupoRepositoryPort.registrarEstudianteEnGrupo(
                RegistrarEstudianteRepositoryMapper.toRepositoryDTO(domain, passwordParaPersistir)
        );
        return RegistrarEstudianteRepositoryMapper.toUseCaseEntity(resultado);
    }

    private boolean validarConflictoIdentidadUsuario(final RegistrarEstudianteDomain domain) {
        final Optional<UsuarioIdentidadRepositoryProjection> usuarioPorCorreo =
                usuarioRepositoryPort.consultarUsuarioPorCorreo(domain.getCorreo());
        final Optional<UsuarioIdentidadRepositoryProjection> usuarioPorIdentificacion =
                usuarioRepositoryPort.consultarUsuarioPorIdentificacion(
                        domain.getTipoIdentificacionId(),
                        domain.getNumeroIdentificacion()
                );

        if (usuarioPorCorreo.isPresent()
                && usuarioPorIdentificacion.isPresent()
                && !usuarioPorCorreo.get().id().equals(usuarioPorIdentificacion.get().id())) {
            throw new ConflictException(EstudianteErrorCode.ERR_IDENTIDAD_USUARIO_CONFLICTO);
        }
        return usuarioPorCorreo.isPresent() || usuarioPorIdentificacion.isPresent();
    }

    private String resolverPasswordParaPersistir(
            final RegistrarEstudianteDomain domain,
            final boolean usuarioExistente
    ) {
        if (usuarioExistente) {
            return null;
        }
        return passwordEncoderPort.encode(
                PasswordRegistroRule.resolverCredencialNueva(domain.getPassword(), domain.getNumeroIdentificacion())
        );
    }
}