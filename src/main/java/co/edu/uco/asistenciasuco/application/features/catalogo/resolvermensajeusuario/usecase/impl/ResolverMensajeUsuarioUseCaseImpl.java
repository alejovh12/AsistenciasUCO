package co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.usecase.ResolverMensajeUsuarioUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

/**
 * Consulta el catalogo de mensajes y degrada a {@link Optional#empty()} ante ausencia, mensaje vacio o fallo del provider.
 */
public final class ResolverMensajeUsuarioUseCaseImpl implements ResolverMensajeUsuarioUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResolverMensajeUsuarioUseCaseImpl.class);

    private final MessageCatalogPort messageCatalogPort;

    public ResolverMensajeUsuarioUseCaseImpl(final MessageCatalogPort messageCatalogPort) {
        this.messageCatalogPort = Objects.requireNonNull(messageCatalogPort, "El puerto de salida MessageCatalogPort es obligatorio.");
    }

    @Override
    public Optional<String> execute(final String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        try {
            final Optional<String> mensaje = messageCatalogPort.findUserMessage(codigo);
            if (mensaje == null) {
                return Optional.empty();
            }
            return mensaje.filter(texto -> !texto.isBlank());
        } catch (final RuntimeException e) {
            LOGGER.debug("No se pudo resolver mensaje en catalogo. codigo={}, causa={}", codigo, e.getClass().getSimpleName());
            return Optional.empty();
        }
    }
}
