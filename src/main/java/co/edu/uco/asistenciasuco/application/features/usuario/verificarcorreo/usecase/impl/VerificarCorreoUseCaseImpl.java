package co.edu.uco.asistenciasuco.application.features.usuario.verificarcorreo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.usuario.domain.rules.PasswordRegistroRule;
import co.edu.uco.asistenciasuco.application.features.usuario.verificarcorreo.usecase.VerificarCorreoUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.VerificacionCorreoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.util.TokenHashHelper;

import java.util.Objects;
import java.util.UUID;

/**
 * IDENT-001: orden de la verificacion (DB primero, IdP despues, consumo al final):
 * <ol>
 *   <li>se consulta el token sin consumirlo;</li>
 *   <li>IDENT-002: si la cuenta no tiene contrasena (registrada por otra persona) se exige la que define su titular, se
 *       valida con {@link PasswordRegistroRule} y se fija como definitiva; sin ella responde {@code VCE_009} y el token
 *       sigue vigente;</li>
 *   <li>se habilita la cuenta y se marca el correo verificado en el IdP (idempotente);</li>
 *   <li>solo entonces se consume el token y se marca {@code correoConfirmado} en la base de datos.</li>
 * </ol>
 * Si el IdP falla el token sigue vigente y el enlace se puede reintentar. Cualquier token invalido (formato incorrecto,
 * inexistente, vencido, usado o revocado) produce exactamente la misma respuesta (VCE_002).
 */
public final class VerificarCorreoUseCaseImpl implements VerificarCorreoUseCase {

    static final String CODIGO_TOKEN_INVALIDO = "VCE_002";
    static final String CODIGO_EXITO = "VCE_006";
    static final String CODIGO_REQUIERE_CONTRASENA = "VCE_009";

    private final VerificacionCorreoRepositoryPort repositorio;
    private final IdentityProviderPort identityProviderPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final MessageCatalogPort mensajes;

    public VerificarCorreoUseCaseImpl(
            final VerificacionCorreoRepositoryPort repositorio,
            final IdentityProviderPort identityProviderPort,
            final UsuarioRepositoryPort usuarioRepositoryPort,
            final MessageCatalogPort mensajes
    ) {
        this.repositorio = Objects.requireNonNull(repositorio, "VerificacionCorreoRepositoryPort es obligatorio.");
        this.identityProviderPort = Objects.requireNonNull(identityProviderPort, "IdentityProviderPort es obligatorio.");
        this.usuarioRepositoryPort = Objects.requireNonNull(usuarioRepositoryPort, "UsuarioRepositoryPort es obligatorio.");
        this.mensajes = Objects.requireNonNull(mensajes, "MessageCatalogPort es obligatorio.");
    }

    @Override
    public String execute(final String token, final String contrasena) {
        if (!TokenHashHelper.esFormatoValido(token)) {
            throw tokenInvalido();
        }
        final UUID idUsuario = repositorio.consultarUsuarioPorToken(token).orElseThrow(this::tokenInvalido);
        final CuentaIdentidadDTO cuenta = buscarCuenta(idUsuario);
        if (!tieneContrasena(cuenta)) {
            establecerContrasena(idUsuario, cuenta, contrasena);
        }
        habilitarCuenta(cuenta);
        repositorio.confirmar(token);
        return mensajes.getUserMessage(CODIGO_EXITO);
    }

    private CuentaIdentidadDTO buscarCuenta(final UUID idUsuario) {
        try {
            return identityProviderPort.buscarCuentaPorIdUsuario(idUsuario)
                    .orElseThrow(() -> new InternalApplicationException(
                            "No existe la cuenta de identidad del usuario que verifica su correo."));
        } catch (final IdentityProviderPort.IdentityProviderException exception) {
            throw new InternalApplicationException(
                    "No fue posible consultar la cuenta al verificar el correo. El enlace sigue siendo valido.", exception);
        }
    }

    private boolean tieneContrasena(final CuentaIdentidadDTO cuenta) {
        try {
            return identityProviderPort.tieneContrasena(cuenta.idExterno());
        } catch (final IdentityProviderPort.IdentityProviderException exception) {
            throw new InternalApplicationException(
                    "No fue posible consultar las credenciales de la cuenta. El enlace sigue siendo valido.", exception);
        }
    }

    /** IDENT-002: la politica se valida antes de tocar el IdP; un rechazo no consume el token. */
    private void establecerContrasena(final UUID idUsuario, final CuentaIdentidadDTO cuenta, final String contrasena) {
        if (contrasena == null || contrasena.isBlank()) {
            throw new ValidationException(CODIGO_REQUIERE_CONTRASENA, mensajes.getUserMessage(CODIGO_REQUIERE_CONTRASENA));
        }
        final Integer numeroIdentificacion = usuarioRepositoryPort.consultarUsuarioPorId(idUsuario)
                .map(UsuarioIdentidadRepositoryProjection::numeroIdentificacion)
                .orElseThrow(() -> new InternalApplicationException(
                        "No existe el usuario institucional que activa su cuenta."));
        final String valida = PasswordRegistroRule.resolver(contrasena, numeroIdentificacion);
        try {
            identityProviderPort.establecerContrasena(cuenta.idExterno(), valida);
        } catch (final IdentityProviderPort.IdentityProviderException exception) {
            throw new InternalApplicationException(
                    "No fue posible establecer la contrasena al activar la cuenta. El enlace sigue siendo valido.", exception);
        }
    }

    private void habilitarCuenta(final CuentaIdentidadDTO cuenta) {
        try {
            identityProviderPort.confirmarCorreoCuenta(cuenta.idExterno());
        } catch (final IdentityProviderPort.IdentityProviderException exception) {
            throw new InternalApplicationException(
                    "No fue posible habilitar la cuenta al verificar el correo. El enlace sigue siendo valido.", exception);
        }
    }

    private ValidationException tokenInvalido() {
        return new ValidationException(CODIGO_TOKEN_INVALIDO, mensajes.getUserMessage(CODIGO_TOKEN_INVALIDO));
    }
}
