package co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.domain.DocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.mapper.DocenteProgramaMapper;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.usecase.VincularDocenteProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.usecase.domain.VincularDocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.usuario.domain.rules.PasswordRegistroRule;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.VincularDocenteProgramaCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocenteProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocentesProgramaFiltro;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * HU091. Orden: ambito del coordinador -> validacion de la contrasena inicial -> DB (transaccional, la fachada del SP
 * valida RBAC y titularidad) -> aprovisionamiento IAM tras confirmar en DB. Si Keycloak falla no se compensa en DB (D10):
 * el vinculo queda y repetir el alta con la misma persona solo reintenta el aprovisionamiento (D14).
 */
public final class VincularDocenteProgramaUseCaseImpl implements VincularDocenteProgramaUseCase {

    private final InstitutionalScopePort scopePort;
    private final DocenteProgramaCommandPort commandPort;
    private final DocenteProgramaQueryPort queryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final IdentityProviderPort identityProviderPort;

    public VincularDocenteProgramaUseCaseImpl(
            final InstitutionalScopePort scopePort,
            final DocenteProgramaCommandPort commandPort,
            final DocenteProgramaQueryPort queryPort,
            final UsuarioRepositoryPort usuarioRepositoryPort,
            final PasswordEncoderPort passwordEncoderPort,
            final IdentityProviderPort identityProviderPort
    ) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.commandPort = Objects.requireNonNull(commandPort, "DocenteProgramaCommandPort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "DocenteProgramaQueryPort es obligatorio.");
        this.usuarioRepositoryPort = Objects.requireNonNull(usuarioRepositoryPort, "UsuarioRepositoryPort es obligatorio.");
        this.passwordEncoderPort = Objects.requireNonNull(passwordEncoderPort, "PasswordEncoderPort es obligatorio.");
        this.identityProviderPort = Objects.requireNonNull(identityProviderPort, "IdentityProviderPort es obligatorio.");
    }

    @Override
    public DocenteProgramaDomain execute(final VincularDocenteProgramaDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para vincular docente es obligatorio.");
        }
        final UUID actor = domain.actorUsuarioId();
        final UUID idPrograma = scopePort.findProgramaIdByCoordinadorUsuario(actor)
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el programa del coordinador autenticado."));

        final boolean modoNuevo = domain.idDocente() == null && domain.idUsuario() == null;
        final String passwordInicial = modoNuevo
                ? PasswordRegistroRule.resolverCredencialNueva(domain.password(), domain.numeroIdentificacion())
                : null;

        final DocenteProgramaProjection vinculo = modoNuevo
                ? vincularPersonaNueva(domain, idPrograma, passwordInicial)
                : vincularExistente(domain, idPrograma);

        aprovisionarIdentidad(vinculo, passwordInicial);
        return DocenteProgramaMapper.toDomain(vinculo);
    }

    private DocenteProgramaProjection vincularPersonaNueva(
            final VincularDocenteProgramaDomain domain, final UUID idPrograma, final String passwordInicial) {
        final String correo = domain.correo() == null ? null : domain.correo().trim();
        final Optional<UsuarioIdentidadRepositoryProjection> preexistente = usuarioRepositoryPort.consultarUsuarioPorCorreo(correo);
        if (preexistente.isPresent()) {
            final Optional<DocenteProgramaProjection> yaVinculado = releer(domain.actorUsuarioId(), idPrograma, null, preexistente.get().id());
            if (yaVinculado.isPresent()) {
                return yaVinculado.get();
            }
        }
        commandPort.vincular(new VincularDocenteProgramaCommand(null, null, domain.idTipoIdentificacion(),
                domain.numeroIdentificacion(), domain.primerNombre(), domain.segundoNombre(), domain.primerApellido(),
                domain.segundoApellido(), correo, passwordEncoderPort.encode(passwordInicial),
                domain.idTipoVinculacion(), idPrograma, domain.actorUsuarioId()));
        final UsuarioIdentidadRepositoryProjection usuario = usuarioRepositoryPort.consultarUsuarioPorCorreo(correo)
                .orElseThrow(() -> new InternalApplicationException(
                        "No fue posible leer la persona registrada despues de vincular al docente."));
        return releer(domain.actorUsuarioId(), idPrograma, null, usuario.id()).orElseThrow(this::vinculoNoEncontrado);
    }

    private DocenteProgramaProjection vincularExistente(final VincularDocenteProgramaDomain domain, final UUID idPrograma) {
        commandPort.vincular(new VincularDocenteProgramaCommand(domain.idDocente(), domain.idUsuario(), null, null, null,
                null, null, null, null, null, domain.idTipoVinculacion(), idPrograma, domain.actorUsuarioId()));
        return releer(domain.actorUsuarioId(), idPrograma, domain.idDocente(), domain.idUsuario())
                .orElseThrow(this::vinculoNoEncontrado);
    }

    private Optional<DocenteProgramaProjection> releer(
            final UUID actor, final UUID idPrograma, final UUID idDocente, final UUID idUsuario) {
        return queryPort.consultar(actor, new DocentesProgramaFiltro(idPrograma, 1, 1, null, null, null, idDocente, idUsuario))
                .elementos().stream().findFirst();
    }

    private void aprovisionarIdentidad(final DocenteProgramaProjection vinculo, final String passwordInicial) {
        try {
            identityProviderPort.crearCuenta(new CrearCuentaIdentidadDTO(
                    String.valueOf(vinculo.numeroIdentificacion()),
                    vinculo.idUsuario(),
                    vinculo.correo(),
                    vinculo.primerNombre(),
                    vinculo.primerApellido(),
                    passwordInicial,
                    InstitutionalRole.DOCENTE));
        } catch (final IdentityProviderPort.IdentityProviderException exception) {
            throw new InternalApplicationException(
                    "El docente quedo vinculado, pero no fue posible completar el aprovisionamiento de su identidad.", exception);
        }
    }

    private InternalApplicationException vinculoNoEncontrado() {
        return new InternalApplicationException("No fue posible leer el vinculo del docente despues de registrarlo.");
    }
}
