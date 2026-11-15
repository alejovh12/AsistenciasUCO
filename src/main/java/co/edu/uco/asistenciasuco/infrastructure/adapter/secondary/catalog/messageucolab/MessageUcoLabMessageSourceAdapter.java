package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.messageucolab;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogSourcePort;
import co.edu.uco.messageucolab.client.api.MessageReader;
import co.edu.uco.messageucolab.client.api.model.MessageCollection;
import co.edu.uco.messageucolab.client.api.model.MessageDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * INT-003: lee de MessageUcoLab (con la identidad de servicio, via {@link MessageReader} del starter) los textos que se
 * reflejan en la BD.
 *
 * <p>Codigo canonico: el catalogo se cargo con el titulo {@code "<CANONICO> - <descripcion>"} (o solo el canonico si es
 * largo), generado por {@code AsistenciasUCO-Infra/components/messages/build-message-catalog.py}. El candidato se toma del
 * titulo y solo se acepta si su traduccion con {@link MessageUcoLabCodeMapper} (normalizacion y alias) da exactamente el
 * codigo del mensaje: un titulo editado a mano o un mensaje ajeno a Asistencias no se confunde con otro codigo.</p>
 */
public class MessageUcoLabMessageSourceAdapter implements MessageCatalogSourcePort {

    static final String SEPARADOR_TITULO = " - ";
    static final Duration TIMEOUT_MINIMO_LISTADO = Duration.ofSeconds(30);

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageUcoLabMessageSourceAdapter.class);

    private final MessageReader reader;
    private final MessageUcoLabCodeMapper codeMapper;
    private final Duration timeout;
    private final Duration listTimeout;

    public MessageUcoLabMessageSourceAdapter(final MessageReader reader, final MessageUcoLabCodeMapper codeMapper,
                                             final Duration timeout) {
        this.reader = Objects.requireNonNull(reader, "MessageReader de MessageUcoLab es obligatorio.");
        this.codeMapper = Objects.requireNonNull(codeMapper, "El mapeador de codigos es obligatorio.");
        this.timeout = Objects.requireNonNull(timeout, "El timeout es obligatorio.");
        this.listTimeout = timeout.compareTo(TIMEOUT_MINIMO_LISTADO) > 0 ? timeout : TIMEOUT_MINIMO_LISTADO;
    }

    @Override
    public Optional<MensajeExterno> obtener(final String codigoExterno) {
        if (codigoExterno == null || codigoExterno.isBlank()) {
            return Optional.empty();
        }
        try {
            final MessageDetails details = reader.getDetails(codigoExterno.trim()).block(timeout);
            return traducir(details);
        } catch (final RuntimeException e) {
            LOGGER.warn("MessageUcoLab no entrego el mensaje del aviso. codigo={}, causa={}",
                    codigoExterno, e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    @Override
    public List<MensajeExterno> listar() {
        final MessageCollection coleccion;
        try {
            coleccion = reader.getAll().block(listTimeout);
        } catch (final RuntimeException e) {
            LOGGER.warn("MessageUcoLab no entrego el catalogo para reconciliar. causa={}", e.getClass().getSimpleName());
            return List.of();
        }
        if (coleccion == null) {
            return List.of();
        }
        final List<MensajeExterno> mensajes = new ArrayList<>(coleccion.messages().size());
        for (final MessageDetails details : coleccion.messages()) {
            traducir(details).ifPresent(mensajes::add);
        }
        return List.copyOf(mensajes);
    }

    Optional<MensajeExterno> traducir(final MessageDetails details) {
        if (details == null || blank(details.code()) || blank(details.content()) || blank(details.title())) {
            return Optional.empty();
        }
        final String titulo = details.title().trim();
        final int separador = titulo.indexOf(SEPARADOR_TITULO);
        final String candidato = (separador > 0 ? titulo.substring(0, separador) : titulo).trim();
        final Optional<String> remoto = codeMapper.toRemoteCode(candidato);
        if (remoto.isEmpty() || !remoto.get().toUpperCase(Locale.ROOT).equals(details.code().trim().toUpperCase(Locale.ROOT))) {
            LOGGER.debug("Mensaje de MessageUcoLab sin codigo canonico de Asistencias. codigo={}", details.code());
            return Optional.empty();
        }
        return Optional.of(new MensajeExterno(candidato, details.content()));
    }

    private static boolean blank(final String value) {
        return value == null || value.isBlank();
    }
}
