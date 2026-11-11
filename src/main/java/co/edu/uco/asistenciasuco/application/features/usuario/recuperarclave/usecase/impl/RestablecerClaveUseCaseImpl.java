package co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.usuario.domain.rules.PasswordRegistroRule;
import co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.usecase.RestablecerClaveUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RecuperacionClaveRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.ExternalIdentityLinkPort;
import co.edu.uco.asistenciasuco.crosscutting.util.TokenHashHelper;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * HU177: define la contrasena nueva. Orden (el mismo de la verificacion de correo, IDENT-001):
 * <ol>
 *   <li>se consulta el enlace sin consumirlo;</li>
 *   <li>se valida la contrasena con la politica institucional ({@link PasswordRegistroRule});</li>
 *   <li>se fija la contrasena en el proveedor de identidad, que ademas cierra las sesiones abiertas;</li>
 *   <li>solo entonces se consume el enlace.</li>
 * </ol>
 * Si el proveedor de identidad falla, el enlace sigue vigente y se puede reintentar. Cualquier enlace que no sirve
 * (formato incorrecto, inexistente, vencido, usado o revocado) produce exactamente la misma respuesta (RCL_002). La
 * contrasena nunca se guarda en la base de datos ni se escribe en logs.
 */
public final class RestablecerClaveUseCaseImpl implements RestablecerClaveUseCase {

    static final String CODIGO_ENLACE_INVALIDO = "RCL_002";
    static final String CODIGO_EXITO = "RCL_003";

    private final RecuperacionClaveRepositoryPort repositorio;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final IdentityProviderPort identityProviderPort;
    private final ExternalIdentityLinkPort externalIdentityLinkPort;
    private final MessageCatalogPort mensajes;

    public RestablecerClaveUseCaseImpl(
            final RecuperacionClaveRepositoryPort repositorio,
            final UsuarioRepositoryPort usuarioRepositoryPort,
            final IdentityProviderPort identityProviderPort,
            final ExternalIdentityLinkPort externalIdentityLinkPort,
            final MessageCatalogPort mensajes
    ) {
        this.repositorio = Objects.requireNonNull(repositorio, "RecuperacionClaveRepositoryPort es obligatorio.");
        this.usuarioRepositoryPort = Objects.requireNonNull(usuarioRepositoryPort, "UsuarioRepositoryPort es obligatorio.");
        this.identityProviderPort = Objects.requireNonNull(identityProviderPort, "IdentityProviderPort es obligatorio.");
        this.externalIdentityLinkPort = Objects.requireNonNull(externalIdentityLinkPort, "ExternalIdentityLinkPort es obligatorio.");
        this.mensajes = Objects.requireNonNull(mensajes, "MessageCatalogPort es obligatorio.");
    }

    @Override
    public String execute(final String token, final String claveNueva) {
        if (!TokenHashHelper.esFormatoValido(token)) {
            throw enlaceInvalido();
        }
        final UUID idUsuario = repositorio.consultarUsuarioPorToken(token).orElseThrow(this::enlaceInvalido);
        final Integer numeroIdentificacion = usuarioRepositoryPort.consultarUsuarioPorId(idUsuario)
                .map(UsuarioIdentidadRepositoryProjection::numeroIdentificacion)
                .orElseThrow(this::enlaceInvalido);
        final String clave = PasswordRegistroRule.resolverCredencialNueva(claveNueva, numeroIdentificacion);

        fijarClave(idUsuario, clave);
        repositorio.consumir(token);

        return mensajes.getUserMessage(CODIGO_EXITO);
    }

    private void fijarClave(final UUID idUsuario, final String clave) {
        try {
            identityProviderPort.establecerClave(ubicarCuenta(idUsuario), clave);
        } catch (final IdentityProviderPort.IdentityProviderException exception) {
            throw new InternalApplicationException(
                    "No fue posible definir la contrasena en el proveedor de identidad. El enlace sigue siendo valido.", exception);
        }
    }

    /**
     * Ubica la cuenta en el proveedor igual que la sincronizacion de acceso: primero por el vinculo vigente
     * {@code issuer + subject} (autoritativo: lo tiene toda persona que ya inicio sesion); si aun no lo hay, por el
     * atributo institucional con coincidencia exacta.
     */
    private String ubicarCuenta(final UUID idUsuario) {
        final List<String> sujetos = externalIdentityLinkPort.findSujetosVigentes(idUsuario);
        if (sujetos.size() == 1) {
            return sujetos.getFirst();
        }
        return identityProviderPort.buscarCuentaPorIdUsuario(idUsuario)
                .map(CuentaIdentidadDTO::idExterno)
                .orElseThrow(() -> new InternalApplicationException(
                        "No existe la cuenta de identidad del usuario que define su contrasena."));
    }

    private ValidationException enlaceInvalido() {
        return new ValidationException(CODIGO_ENLACE_INVALIDO, mensajes.getUserMessage(CODIGO_ENLACE_INVALIDO));
    }
}
