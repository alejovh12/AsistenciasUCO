package co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.CrearCoordinadorUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.domain.CrearCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.features.usuario.domain.rules.PasswordRegistroRule;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorCommandPort;
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

public final class CrearCoordinadorUseCaseImpl implements CrearCoordinadorUseCase {

    private final CoordinadorCommandPort commandPort;
    private final InstitutionalScopePort scopePort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final IdentityProviderPort identityProviderPort;

    public CrearCoordinadorUseCaseImpl(final CoordinadorCommandPort commandPort, final InstitutionalScopePort scopePort) {
        this(commandPort, scopePort, null, null, null);
    }

    public CrearCoordinadorUseCaseImpl(
            final CoordinadorCommandPort commandPort,
            final InstitutionalScopePort scopePort,
            final UsuarioRepositoryPort usuarioRepositoryPort,
            final PasswordEncoderPort passwordEncoderPort,
            final IdentityProviderPort identityProviderPort
    ) {
        this.commandPort = Objects.requireNonNull(commandPort, "CoordinadorCommandPort es obligatorio.");
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
        this.identityProviderPort = identityProviderPort;
    }

    @Override
    public void execute(final CrearCoordinadorDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para crear coordinador es obligatorio.");
        }
        final UUID facultad = scopePort.findFacultadIdByDecanoUsuario(domain.getUsuario())
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver la facultad del decano autenticado."));

        final Optional<UsuarioIdentidadRepositoryProjection> usuarioPreexistente =
                resolverUsuarioPreexistente(domain);

        validarContrasenaObligatoria(domain, usuarioPreexistente.isPresent());

        final String credencialInicial = passwordEncoderPort == null
                ? domain.getPassword()
                : resolverCredencialInicial(domain);
        final String passwordParaPersistir;
        if (usuarioPreexistente.isPresent()) {
            passwordParaPersistir = null;
        } else if (passwordEncoderPort == null) {
            passwordParaPersistir = domain.getPassword();
        } else {
            passwordParaPersistir = passwordEncoderPort.encode(credencialInicial);
        }

        commandPort.crearCoordinador(UUID.randomUUID(), domain.getNumeroIdentificacion(), domain.getPrimerNombre(),
                domain.getSegundoNombre(), domain.getPrimerApellido(), domain.getSegundoApellido(), domain.getCorreo(),
                domain.getIdPrograma(), facultad, passwordParaPersistir, domain.getUsuario());

        if (identityProviderPort != null && usuarioRepositoryPort != null) {
            final UsuarioIdentidadRepositoryProjection u = resolverUsuarioPostCommand(domain, usuarioPreexistente);
            try {
                identityProviderPort.crearCuenta(new CrearCuentaIdentidadDTO(
                        String.valueOf(u.numeroIdentificacion()),
                        u.id(),
                        u.correo(),
                        u.primerNombre(),
                        u.primerApellido(),
                        credencialInicial,
                        InstitutionalRole.COORDINADOR
                ));
            } catch (final IdentityProviderPort.IdentityProviderException exception) {
                throw new InternalApplicationException(
                        "No fue posible completar el aprovisionamiento de identidad del coordinador registrado.", exception
                );
            }
        }
    }

    /**
     * Resuelve la identidad por las dos claves institucionales antes de invocar el SP.
     * Un reintento con la misma persona debe reutilizar el usuario; si correo y documento
     * apuntan a personas distintas, no se permite que el SP elija una de ellas.
     */
    private Optional<UsuarioIdentidadRepositoryProjection> resolverUsuarioPreexistente(
            final CrearCoordinadorDomain domain) {
        if (usuarioRepositoryPort == null) {
            return Optional.empty();
        }
        final Optional<UsuarioIdentidadRepositoryProjection> porCorreo = consultarPorCorreo(domain);
        final Optional<UsuarioIdentidadRepositoryProjection> porDocumento = consultarPorDocumento(domain);
        if (porCorreo.isEmpty() && porDocumento.isEmpty()) {
            return Optional.empty();
        }
        if (porCorreo.isPresent() && porDocumento.isEmpty()) {
            throw new ConflictException(UsuarioErrorCode.ERR_UNICIDAD_CORREO);
        }
        if (porCorreo.isEmpty()) {
            throw new ConflictException(UsuarioErrorCode.ERR_UNICIDAD_DOCUMENTO);
        }
        validarIdentidad(proyeccionValida(porCorreo), proyeccionValida(porDocumento));
        return porCorreo;
    }

    private UsuarioIdentidadRepositoryProjection resolverUsuarioPostCommand(
            final CrearCoordinadorDomain domain,
            final Optional<UsuarioIdentidadRepositoryProjection> usuarioPreexistente) {
        final Optional<UsuarioIdentidadRepositoryProjection> usuario = usuarioPreexistente.isPresent()
                ? usuarioRepositoryPort.consultarUsuarioPorId(usuarioPreexistente.get().id())
                : consultarPorDocumento(domain).or(() -> consultarPorCorreo(domain));
        return usuario.filter(this::esIdentidadCanonica)
                .orElseThrow(() -> new InternalApplicationException(
                        "No fue posible resolver la identidad institucional canonica del coordinador despues del registro."
                ));
    }

    private Optional<UsuarioIdentidadRepositoryProjection> consultarPorCorreo(final CrearCoordinadorDomain domain) {
        if (domain.getCorreo() == null) {
            return Optional.empty();
        }
        return usuarioRepositoryPort.consultarUsuarioPorCorreo(domain.getCorreo().trim());
    }

    private Optional<UsuarioIdentidadRepositoryProjection> consultarPorDocumento(final CrearCoordinadorDomain domain) {
        if (domain.getNumeroIdentificacion() == null || domain.getNumeroIdentificacion().isBlank()) {
            return Optional.empty();
        }
        try {
            return usuarioRepositoryPort.consultarUsuarioPorNumeroIdentificacion(
                    Integer.valueOf(domain.getNumeroIdentificacion().trim()));
        } catch (final NumberFormatException _) {
            return Optional.empty();
        }
    }

    private void validarIdentidad(
            final UsuarioIdentidadRepositoryProjection porCorreo,
            final UsuarioIdentidadRepositoryProjection porDocumento) {
        if (!porCorreo.id().equals(porDocumento.id())) {
            throw new ConflictException(UsuarioErrorCode.ERR_IDENTIDAD_USUARIO_CONFLICTO);
        }
    }

    private boolean esIdentidadCanonica(final UsuarioIdentidadRepositoryProjection usuario) {
        return usuario != null
                && usuario.id() != null
                && usuario.numeroIdentificacion() != null
                && hasText(usuario.primerNombre())
                && hasText(usuario.primerApellido())
                && hasText(usuario.correo());
    }

    private UsuarioIdentidadRepositoryProjection proyeccionValida(
            final Optional<UsuarioIdentidadRepositoryProjection> usuario) {
        return usuario.filter(this::esIdentidadCanonica)
                .orElseThrow(() -> new InternalApplicationException(
                        "No fue posible validar la identidad institucional del coordinador."
                ));
    }

    private static boolean hasText(final String value) {
        return value != null && !value.isBlank();
    }

    /**
     * HU121-HU122 D5: la contrasena se asigna siempre en la creacion; no existe una por defecto derivada del documento.
     * Un usuario ya registrado (por correo) no necesita contrasena nueva.
     */
    private void validarContrasenaObligatoria(final CrearCoordinadorDomain domain, final boolean usuarioPreexistente) {
        final boolean sinContrasena = domain.getPassword() == null || domain.getPassword().isBlank();
        if (sinContrasena && !usuarioPreexistente && passwordEncoderPort != null) {
            throw new ValidationException("La contrasena inicial del coordinador es obligatoria.");
        }
    }

    private static String resolverCredencialInicial(final CrearCoordinadorDomain domain) {
        Integer numeroId = null;
        if (domain.getNumeroIdentificacion() != null && !domain.getNumeroIdentificacion().isBlank()) {
            try {
                numeroId = Integer.valueOf(domain.getNumeroIdentificacion().trim());
            } catch (final NumberFormatException _) {
                numeroId = null;
            }
        }
        final String rawPassword = domain.getPassword() == null ? "" : domain.getPassword().trim();
        return PasswordRegistroRule.resolverCredencialNueva(rawPassword, numeroId != null ? numeroId : 10000000);
    }
}
