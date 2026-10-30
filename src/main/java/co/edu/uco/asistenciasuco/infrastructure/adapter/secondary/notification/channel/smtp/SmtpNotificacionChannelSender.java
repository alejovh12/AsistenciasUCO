package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.channel.smtp;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contact.ContactoDestinatario;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contact.ContactoDestinatarioResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.DestinatarioRef;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionChannelSender;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionNoEntregableException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionPersistida;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.ContenidoRenderizado;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.PlantillaRenderer;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SmtpNotificationProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Canal de salida SMTP para el outbox de notificaciones (CONTRACT §4.3).
 * Resuelve contactos al momento de entregar, renderiza plantillas, aplica tope de envios y agrupa en BCC.
 * Nunca registra en logs direcciones de correo, asunto ni cuerpo renderizado (D12/D13).
 */
public class SmtpNotificacionChannelSender implements NotificacionChannelSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmtpNotificacionChannelSender.class);
    private static final Pattern PATRON_CORREO_SEGURO =
            Pattern.compile("^[^@\\s\\r\\n]+@[^@\\s\\r\\n]+\\.[^@\\s\\r\\n]+$");

    private final ContactoDestinatarioResolver contactoResolver;
    private final PlantillaRenderer plantillaRenderer;
    private final CorreoTransport correoTransport;
    private final TopeEnvios topeEnvios;
    private final SmtpNotificationProperties properties;
    private final MeterRegistry meterRegistry;

    private final Counter sendSentCounter;
    private final Counter sendRetryCounter;
    private final Counter sendPermanentCounter;
    private final Counter sendNoRecipientsCounter;
    private final Counter recipientsSentCounter;
    private final Counter recipientsOmittedCounter;

    public SmtpNotificacionChannelSender(
            final ContactoDestinatarioResolver contactoResolver,
            final PlantillaRenderer plantillaRenderer,
            final CorreoTransport correoTransport,
            final TopeEnvios topeEnvios,
            final SmtpNotificationProperties properties,
            final MeterRegistry meterRegistry
    ) {
        this.contactoResolver = Objects.requireNonNull(contactoResolver, "ContactoDestinatarioResolver es obligatorio.");
        this.plantillaRenderer = Objects.requireNonNull(plantillaRenderer, "PlantillaRenderer es obligatorio.");
        this.correoTransport = Objects.requireNonNull(correoTransport, "CorreoTransport es obligatorio.");
        this.topeEnvios = Objects.requireNonNull(topeEnvios, "TopeEnvios es obligatorio.");
        this.properties = Objects.requireNonNull(properties, "SmtpNotificationProperties es obligatorio.");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "MeterRegistry es obligatorio.");

        this.sendSentCounter = Counter.builder("notification.email.send.total")
                .tag("outcome", "sent")
                .register(meterRegistry);
        this.sendRetryCounter = Counter.builder("notification.email.send.total")
                .tag("outcome", "retry")
                .register(meterRegistry);
        this.sendPermanentCounter = Counter.builder("notification.email.send.total")
                .tag("outcome", "permanent")
                .register(meterRegistry);
        this.sendNoRecipientsCounter = Counter.builder("notification.email.send.total")
                .tag("outcome", "no_recipients")
                .register(meterRegistry);
        this.recipientsSentCounter = Counter.builder("notification.email.recipients.total")
                .tag("outcome", "sent")
                .register(meterRegistry);
        this.recipientsOmittedCounter = Counter.builder("notification.email.recipients.total")
                .tag("outcome", "omitted")
                .register(meterRegistry);
    }

    @Override
    public boolean disponible() {
        return topeEnvios.disponible();
    }

    @Override
    public void enviar(final NotificacionPersistida notificacion) throws NotificacionNoEntregableException {
        Objects.requireNonNull(notificacion, "NotificacionPersistida es obligatoria.");

        final List<UUID> idsSolicitados = extraerIdsUsuarios(notificacion.destinatarios());
        final int totalSolicitados = idsSolicitados.size();

        final List<ContactoDestinatario> contactos = idsSolicitados.isEmpty()
                ? List.of()
                : contactoResolver.consultarContactos(idsSolicitados);

        final List<String> correosValidos = filtrarCorreosValidos(contactos);
        final int omitidos = Math.max(0, totalSolicitados - correosValidos.size());
        if (omitidos > 0) {
            recipientsOmittedCounter.increment(omitidos);
        }

        if (correosValidos.isEmpty()) {
            sendNoRecipientsCounter.increment();
            LOGGER.warn(
                    "Notificacion sin destinatarios con correo activo: idNotificacion={}, tipo={}, outcome=no_recipients",
                    notificacion.id(),
                    notificacion.tipo()
            );
            throw new NotificacionNoEntregableException(
                    "Ningun destinatario tiene correo electronico activo y valido."
            );
        }

        final ContenidoRenderizado contenido = plantillaRenderer.renderizar(
                notificacion.tipo(),
                notificacion.datos(),
                notificacion.asunto(),
                notificacion.mensaje()
        );
        final String asuntoSaneado = sanearAsunto(contenido.asunto());

        final List<List<String>> bloquesBcc = partirEnBloques(correosValidos, properties.maxBcc());
        for (int idx = 0; idx < bloquesBcc.size(); idx++) {
            if (!topeEnvios.intentarConsumir()) {
                sendRetryCounter.increment();
                throw new IllegalStateException("Tope de envios SMTP agotado antes de enviar bloque.");
            }
            final List<String> bloque = bloquesBcc.get(idx);
            final String messageId = "<notif-" + notificacion.id() + "-" + idx + "@uco.edu.co>";
            final CorreoSaliente correo = new CorreoSaliente(
                    properties.fromAddress(),
                    properties.fromName(),
                    properties.fromAddress(),
                    bloque,
                    asuntoSaneado,
                    contenido.textoPlano(),
                    contenido.html(),
                    messageId
            );

            final long inicioNanos = System.nanoTime();
            try {
                correoTransport.enviar(correo);
                registrarDuracion("sent", System.nanoTime() - inicioNanos);
                sendSentCounter.increment();
                recipientsSentCounter.increment(bloque.size());
            } catch (final NotificacionNoEntregableException ex) {
                registrarDuracion("permanent", System.nanoTime() - inicioNanos);
                sendPermanentCounter.increment();
                LOGGER.warn(
                        "Fallo permanente SMTP al enviar notificacion: idNotificacion={}, tipo={}, outcome=permanent",
                        notificacion.id(),
                        notificacion.tipo()
                );
                throw ex;
            } catch (final RuntimeException ex) {
                registrarDuracion("retry", System.nanoTime() - inicioNanos);
                sendRetryCounter.increment();
                LOGGER.warn(
                        "Fallo transitorio SMTP al enviar notificacion: idNotificacion={}, tipo={}, outcome=retry",
                        notificacion.id(),
                        notificacion.tipo()
                );
                throw ex;
            }
        }

        LOGGER.info(
                "Notificacion enviada por SMTP: idNotificacion={}, tipo={}, destinatarios={}, origenPlantilla={}, outcome=sent",
                notificacion.id(),
                notificacion.tipo(),
                correosValidos.size(),
                contenido.origen()
        );
    }

    private List<UUID> extraerIdsUsuarios(final List<DestinatarioRef> destinatarios) {
        final Set<UUID> unicos = new LinkedHashSet<>();
        for (final DestinatarioRef ref : destinatarios) {
            if (ref.idUsuario() != null) {
                unicos.add(ref.idUsuario());
            }
        }
        return List.copyOf(unicos);
    }

    private List<String> filtrarCorreosValidos(final List<ContactoDestinatario> contactos) {
        if (contactos == null || contactos.isEmpty()) {
            return List.of();
        }
        final Set<String> validos = new LinkedHashSet<>();
        for (final ContactoDestinatario c : contactos) {
            if (c == null) {
                continue;
            }
            if (properties.requireConfirmedEmail() && !c.correoConfirmado()) {
                continue;
            }
            final String correo = c.correo();
            if (esCorreoSeguro(correo)) {
                validos.add(correo.trim());
            }
        }
        return List.copyOf(validos);
    }

    static boolean esCorreoSeguro(final String correo) {
        if (correo == null || correo.isBlank()) {
            return false;
        }
        if (correo.indexOf('\r') >= 0 || correo.indexOf('\n') >= 0) {
            return false;
        }
        final String limpio = correo.trim();
        return PATRON_CORREO_SEGURO.matcher(limpio).matches();
    }

    static String sanearAsunto(final String asunto) {
        if (asunto == null) {
            return "";
        }
        return asunto.replace("\r", " ").replace("\n", " ").trim();
    }

    private static List<List<String>> partirEnBloques(final List<String> elementos, final int tamanoMaximo) {
        final List<List<String>> bloques = new ArrayList<>();
        for (int i = 0; i < elementos.size(); i += tamanoMaximo) {
            bloques.add(elementos.subList(i, Math.min(elementos.size(), i + tamanoMaximo)));
        }
        return bloques;
    }

    private void registrarDuracion(final String outcome, final long nanos) {
        Timer.builder("notification.email.send.duration")
                .tag("outcome", outcome)
                .register(meterRegistry)
                .record(nanos, TimeUnit.NANOSECONDS);
    }
}
