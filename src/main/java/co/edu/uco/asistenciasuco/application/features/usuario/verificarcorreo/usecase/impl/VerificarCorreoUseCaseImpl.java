package co.edu.uco.asistenciasuco.application.features.usuario.verificarcorreo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.usuario.verificarcorreo.usecase.VerificarCorreoUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.VerificacionCorreoRepositoryPort;
import co.edu.uco.asistenciasuco.crosscutting.util.TokenHashHelper;

import java.util.Objects;
import java.util.UUID;

/**
 * IDENT-001: orden de la verificacion (DB primero, IdP despues, consumo al final):
 * <ol>
 *   <li>se consulta el token sin consumirlo;</li>
 *   <li>se habilita la cuenta y se marca el correo verificado en el IdP (idempotente);</li>
 *   <li>solo entonces se consume el token y se marca {@code correoConfirmado} en la base de datos.</li>
 * </ol>
 * Si el IdP falla el token sigue vigente y el enlace se puede reintentar. Todo token invalido (formato incorrecto,
 * inexistente, vencido, usado o revocado) produce exactamente la misma respuesta (VCE_002).
 */
public final class VerificarCorreoUseCaseImpl implements VerificarCorreoUseCase {

    static final String CODIGO_TOKEN_INVALIDO = "VCE_002";
    static final String CODIGO_EXITO = "VCE_006";

    private final VerificacionCorreoRepositoryPort repositorio;
    private final IdentityProviderPort identityProviderPort;
    private final MessageCatalogPort mensajes;

    public VerificarCorreoUseCaseImpl(
            final VerificacionCorreoRepositoryPort repositorio,
            final IdentityProviderPort identityProviderPort,
            final MessageCatalogPort mensajes
    ) {
        this.repositorio = Objects.requireNonNull(repositorio, "VerificacionCorreoRepositoryPort es obligatorio.");
        this.identityProviderPort = Objects.requireNonNull(identityProviderPort, "IdentityProviderPort es obligatorio.");
        this.mensajes = Objects.requireNonNull(mensajes, "MessageCatalogPort es obligatorio.");
    }

    @Override
    public String execute(final String token) {
        if (!TokenHashHelper.esFormatoValido(token)) {
            throw tokenInvalido();
        }
        final UUID idUsuario = repositorio.consultarUsuarioPorToken(token).orElseThrow(this::tokenInvalido);

        habilitarCuenta(idUsuario);
        repositorio.confirmar(token);

        return mensajes.getUserMessage(CODIGO_EXITO);
    }

    private void habilitarCuenta(final UUID idUsuario) {
        try {
            final CuentaIdentidadDTO cuenta = identityProviderPort.buscarCuentaPorIdUsuario(idUsuario)
                    .orElseThrow(() -> new InternalApplicationException(
                            "No existe la cuenta de identidad del usuario que verifica su correo."));
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
