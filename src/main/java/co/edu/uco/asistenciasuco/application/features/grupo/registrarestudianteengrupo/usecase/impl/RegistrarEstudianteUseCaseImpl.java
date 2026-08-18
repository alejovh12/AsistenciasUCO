package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.ConflictException;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.RegistrarEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.domain.RegistrarEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.entity.RegistrarEstudianteResultadoEntity;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.mapper.RegistrarEstudianteRepositoryMapper;
import co.edu.uco.asistenciasuco.application.features.usuario.domain.rules.PasswordRegistroRule;
import co.edu.uco.asistenciasuco.application.secondaryports.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.RegistrarEstudianteRepositoryEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.UsuarioIdentidadRepositoryEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.Optional;

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
        if (ObjectHelper.isNull(grupoRepositoryPort)) {
            throw new CrosscuttingException("El puerto de salida GrupoRepositoryPort es obligatorio.");
        }
        if (ObjectHelper.isNull(usuarioRepositoryPort)) {
            throw new CrosscuttingException("El puerto de salida UsuarioRepositoryPort es obligatorio.");
        }
        if (ObjectHelper.isNull(passwordEncoderPort)) {
            throw new CrosscuttingException("El puerto PasswordEncoderPort es obligatorio.");
        }
        this.grupoRepositoryPort = grupoRepositoryPort;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    @Override
    public RegistrarEstudianteResultadoEntity execute(final RegistrarEstudianteDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para registrar estudiante en grupo es obligatorio.");
        }
        final boolean usuarioExistente = validarConflictoIdentidadUsuario(domain);
        final String passwordParaPersistir = resolverPasswordParaPersistir(domain, usuarioExistente);
        final RegistrarEstudianteRepositoryEntity resultado = grupoRepositoryPort.registrarEstudianteEnGrupo(
                RegistrarEstudianteRepositoryMapper.toRepositoryDTO(domain, passwordParaPersistir)
        );
        return RegistrarEstudianteRepositoryMapper.toUseCaseEntity(resultado);
    }

    private boolean validarConflictoIdentidadUsuario(final RegistrarEstudianteDomain domain) {
        final Optional<UsuarioIdentidadRepositoryEntity> usuarioPorCorreo =
                usuarioRepositoryPort.consultarUsuarioPorCorreo(domain.getCorreo());
        final Optional<UsuarioIdentidadRepositoryEntity> usuarioPorIdentificacion =
                usuarioRepositoryPort.consultarUsuarioPorIdentificacion(
                        domain.getTipoIdentificacionId(),
                        domain.getNumeroIdentificacion()
                );

        if (usuarioPorCorreo.isPresent()
                && usuarioPorIdentificacion.isPresent()
                && !usuarioPorCorreo.get().id().equals(usuarioPorIdentificacion.get().id())) {
            throw new ConflictException(
                    "ERR_IDENTIDAD_USUARIO_CONFLICTO",
                    "El correo y la identificacion corresponden a usuarios diferentes."
            );
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
