package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.impl;


import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.RegistrarEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.domain.RegistrarEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.entity.RegistrarEstudianteResultadoEntity;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.mapper.RegistrarEstudianteIdentityMapper;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.mapper.RegistrarEstudianteRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.RegistrarEstudianteRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

/**
 * Implementacion del caso de uso registrar estudiante en grupo.
 *
 * <p>Flujo DB-first: el command contra {@link GrupoRepositoryPort} debe terminar exitosamente
 * antes de invocar {@link IdentityProviderPort}. Un fallo de Identity posterior al commit de DB
 * nunca intenta deshacer la operacion de base de datos (no hay rollback distribuido DB-IdP;
 * ver {@link IdentityProviderPort#eliminarCuenta(String)}).</p>
 */
public final class RegistrarEstudianteUseCaseImpl implements RegistrarEstudianteUseCase {

    private static final InstitutionalRole ROL_ESTUDIANTE = InstitutionalRole.ESTUDIANTE;

    private final GrupoRepositoryPort grupoRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final IdentityProviderPort identityProviderPort;

    public RegistrarEstudianteUseCaseImpl(
            final GrupoRepositoryPort grupoRepositoryPort,
            final UsuarioRepositoryPort usuarioRepositoryPort,
            final PasswordEncoderPort passwordEncoderPort,
            final IdentityProviderPort identityProviderPort
    ) {
        this.grupoRepositoryPort = Objects.requireNonNull(grupoRepositoryPort, "El puerto de salida GrupoRepositoryPort es obligatorio.");
        this.usuarioRepositoryPort = Objects.requireNonNull(usuarioRepositoryPort, "El puerto de salida UsuarioRepositoryPort es obligatorio.");
        this.passwordEncoderPort = Objects.requireNonNull(passwordEncoderPort, "El puerto PasswordEncoderPort es obligatorio.");
        this.identityProviderPort = Objects.requireNonNull(identityProviderPort, "El puerto IdentityProviderPort es obligatorio.");
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

        // DB SUCCESS a partir de aqui: Identity nunca se invoca antes de este punto.
        final UsuarioIdentidadRepositoryProjection usuarioCanonico = resolverUsuarioPostCommand(domain);
        provisionarIdentidad(domain, usuarioCanonico);

        return RegistrarEstudianteRepositoryMapper.toUseCaseEntity(resultado);
    }

    /**
     * Resuelve el UUID institucional definitivo DESPUES del command, usando la identificacion
     * (tipo + numero) como referencia — no el correo, que el SP no garantiza actualizar para un
     * usuario preexistente encontrado por documento (ver docs de esta feature).
     */
    private UsuarioIdentidadRepositoryProjection resolverUsuarioPostCommand(final RegistrarEstudianteDomain domain) {
        return usuarioRepositoryPort.consultarUsuarioPorIdentificacion(
                domain.getTipoIdentificacionId(), domain.getNumeroIdentificacion()
        ).filter(usuario -> usuario.id() != null && usuario.correo() != null && !usuario.correo().isBlank())
        .orElseThrow(() -> new InternalApplicationException(
                "No fue posible resolver la identidad institucional canonica del usuario despues de "
                        + "registrar el estudiante en el grupo."
        ));
    }

    /**
     * Un fallo de Identity en este punto (DB ya confirmada) se transforma en un error controlado
     * de Application — nunca se intenta deshacer el command de DB ni se invoca
     * {@link IdentityProviderPort#eliminarCuenta(String)} como compensacion distribuida.
     */
    private void provisionarIdentidad(
            final RegistrarEstudianteDomain domain,
            final UsuarioIdentidadRepositoryProjection usuarioCanonico
    ) {
        try {
            identityProviderPort.crearCuenta(
                    RegistrarEstudianteIdentityMapper.toCrearCuentaIdentidadDTO(domain, usuarioCanonico, ROL_ESTUDIANTE)
            );
        } catch (final IdentityProviderPort.IdentityProviderException exception) {
            throw new InternalApplicationException(
                    "No fue posible completar el aprovisionamiento de identidad del estudiante registrado.",
                    exception
            );
        }
    }

    private boolean validarConflictoIdentidadUsuario(final RegistrarEstudianteDomain domain) {
        final Optional<UUID> idPorCorreo = RegistrarEstudianteRepositoryMapper.toIdentidadId(
                usuarioRepositoryPort.consultarUsuarioPorCorreo(domain.getCorreo())
        );
        final Optional<UUID> idPorIdentificacion = RegistrarEstudianteRepositoryMapper.toIdentidadId(
                usuarioRepositoryPort.consultarUsuarioPorIdentificacion(
                        domain.getTipoIdentificacionId(),
                        domain.getNumeroIdentificacion()
                )
        );

        if (idPorCorreo.isPresent()
                && idPorIdentificacion.isPresent()
                && !idPorCorreo.get().equals(idPorIdentificacion.get())) {
            throw new ConflictException(UsuarioErrorCode.ERR_IDENTIDAD_USUARIO_CONFLICTO);
        }
        return idPorCorreo.isPresent() || idPorIdentificacion.isPresent();
    }

    private String resolverPasswordParaPersistir(
            final RegistrarEstudianteDomain domain,
            final boolean usuarioExistente
    ) {
        if (usuarioExistente) {
            return null;
        }
        return passwordEncoderPort.encode(domain.resolverCredencialNueva());
    }
}
