package co.edu.uco.asistenciasuco.application.features.catalogo.consultarmensajeinterfaz.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.features.catalogo.consultarmensajeinterfaz.primaryports.ConsultarMensajeInterfazInputPort.MensajeInterfaz;
import co.edu.uco.asistenciasuco.application.features.catalogo.consultarmensajeinterfaz.usecase.ConsultarMensajeInterfazUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * INT-003 (MessageUcoLab 0.3.0, paso 12): entrega a la SPA, sin sesion, el texto de la interfaz que pide por codigo.
 *
 * <p>La ruta es publica, asi que solo responde los codigos de la interfaz ({@code UI} seguido de 3 a 8 digitos, los que
 * genera {@code sync-mensajes-ui-catalogo.mjs} en el frontend): ningun mensaje de negocio ni tecnico del catalogo se puede
 * leer por aqui. Un codigo fuera de ese patron responde lo mismo que un codigo inexistente ({@code GEN_001}, 404), para no
 * revelar que codigos existen. El texto lo resuelve {@link MessageCatalogPort} (proveedor configurado, con su cache y su
 * contingencia).</p>
 */
public final class ConsultarMensajeInterfazUseCaseImpl implements ConsultarMensajeInterfazUseCase {

    private static final Pattern CODIGO_INTERFAZ = Pattern.compile("^UI\\d{3,8}$");
    private static final String RECURSO = "mensaje";

    private final MessageCatalogPort catalogo;

    public ConsultarMensajeInterfazUseCaseImpl(final MessageCatalogPort catalogo) {
        this.catalogo = Objects.requireNonNull(catalogo, "MessageCatalogPort es obligatorio.");
    }

    @Override
    public MensajeInterfaz execute(final String codigo) {
        final String limpio = codigo == null ? "" : codigo.trim();
        if (!CODIGO_INTERFAZ.matcher(limpio).matches()) {
            throw noExiste();
        }
        return catalogo.findUserMessage(limpio)
                .map(String::trim)
                .filter(texto -> !texto.isEmpty())
                .map(texto -> new MensajeInterfaz(limpio, texto))
                .orElseThrow(ConsultarMensajeInterfazUseCaseImpl::noExiste);
    }

    private static ResourceNotFoundException noExiste() {
        return ResourceNotFoundException.conParametros(MensajeUsuarioErrorCode.GEN_001, RECURSO);
    }
}
