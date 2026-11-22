package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.service;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contact.ContactoDestinatario;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contact.ContactoDestinatarioResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.DestinatarioRef;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionChannelSender;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionNoEntregableException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionPersistida;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.verificacion.MarcadorDinamicoProvider;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.NotificacionesServiceProperties;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Canal de salida del outbox hacia el Servicio de Notificaciones UCO. Resuelve los correos al entregar (con
 * {@code requireConfirmedEmail}), completa los marcadores dinamicos (enlace de verificacion o activacion) y registra el aviso
 * con {@code POST /api/v1/notificaciones}. El servicio renderiza, aplica el tope y envia: aqui «entregado» significa que el
 * servicio acepto el aviso (202) o ya lo tenia (200).
 *
 * <p>El {@code idempotencyKey} es el id de la {@code NotificacionSaliente}: un reintento tras un 5xx, un tiempo de espera o
 * una caida de red reusa la misma clave y el servicio nunca duplica el envio. Un 4xx (salvo 401/403/408/429) es un fallo
 * definitivo que guarda el {@code code} del servicio; 401/403 son un error de configuracion (clave API) que se registra como
 * ERROR y se reintenta. Nunca registra correos, asuntos ni cuerpos.</p>
 */
public final class ServicioNotificacionChannelSender implements NotificacionChannelSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicioNotificacionChannelSender.class);
    private static final String RUTA = "/api/v1/notificaciones";
    private static final String METRICA = "notification.service.send.total";
    private static final int MAX_DESTINATARIOS = 200;
    private static final int MAX_ASUNTO = 200;
    private static final int MAX_MENSAJE = 4000;
    private static final int MAX_DATOS = 100;
    private static final int MAX_VALOR_DATO = 2000;
    private static final Pattern CLAVE_DATO = Pattern.compile("[A-Za-z0-9]{1,40}");

    private final NotificacionesServiceClient cliente;
    private final ContactoDestinatarioResolver contactoResolver;
    private final MarcadorDinamicoProvider marcadoresDinamicos;
    private final NotificacionesServiceProperties properties;
    private final Counter aceptadas;
    private final Counter duplicadas;
    private final Counter reintentables;
    private final Counter definitivas;
    private final Counter errorConfiguracion;
    private final Counter sinDestinatarios;

    public ServicioNotificacionChannelSender(
            final NotificacionesServiceClient cliente,
            final ContactoDestinatarioResolver contactoResolver,
            final MarcadorDinamicoProvider marcadoresDinamicos,
            final NotificacionesServiceProperties properties,
            final MeterRegistry meterRegistry
    ) {
        this.cliente = Objects.requireNonNull(cliente, "NotificacionesServiceClient es obligatorio.");
        this.contactoResolver = Objects.requireNonNull(contactoResolver, "ContactoDestinatarioResolver es obligatorio.");
        this.marcadoresDinamicos = Objects.requireNonNull(marcadoresDinamicos, "MarcadorDinamicoProvider es obligatorio.");
        this.properties = Objects.requireNonNull(properties, "NotificacionesServiceProperties es obligatorio.");
        Objects.requireNonNull(meterRegistry, "MeterRegistry es obligatorio.");
        this.aceptadas = contador(meterRegistry, "accepted");
        this.duplicadas = contador(meterRegistry, "duplicate");
        this.reintentables = contador(meterRegistry, "retry");
        this.definitivas = contador(meterRegistry, "permanent");
        this.errorConfiguracion = contador(meterRegistry, "config_error");
        this.sinDestinatarios = contador(meterRegistry, "no_recipients");
    }

    private static Counter contador(final MeterRegistry registro, final String resultado) {
        return Counter.builder(METRICA).tag("outcome", resultado).register(registro);
    }

    /** {@code false} con el circuit breaker abierto: el despachador no reserva ni consume intentos mientras tanto. */
    @Override
    public boolean disponible() {
        return cliente.disponible();
    }

    @Override
    public void enviar(final NotificacionPersistida notificacion) throws NotificacionNoEntregableException {
        Objects.requireNonNull(notificacion, "NotificacionPersistida es obligatoria.");

        final List<UUID> idsSolicitados = extraerIdsUsuarios(notificacion.destinatarios());
        final List<ContactoDestinatario> contactos = idsSolicitados.isEmpty()
                ? List.of()
                : contactoResolver.consultarContactos(idsSolicitados);
        // IDENT-001/IDENT-002: la verificacion y la activacion llegan precisamente a un correo aun sin confirmar.
        final boolean exentoDeConfirmacion = Notificacion.TIPO_VERIFICACION_CORREO.equals(notificacion.tipo())
                || Notificacion.TIPO_ACTIVACION_CUENTA.equals(notificacion.tipo());
        final List<String> correos = correosValidos(contactos, exentoDeConfirmacion);
        if (correos.isEmpty()) {
            sinDestinatarios.increment();
            LOGGER.warn("Notificacion sin destinatarios con correo activo: idNotificacion={}, tipo={}, outcome=no_recipients",
                    notificacion.id(), notificacion.tipo());
            throw new NotificacionNoEntregableException("Ningun destinatario tiene correo electronico activo y valido.");
        }

        // IDENT-001: el token del enlace se emite solo cuando hay a quien enviar.
        final Map<String, String> datos = combinarDatos(notificacion, idsSolicitados);

        for (int bloque = 0; bloque * MAX_DESTINATARIOS < correos.size(); bloque++) {
            final List<String> parte = correos.subList(bloque * MAX_DESTINATARIOS,
                    Math.min(correos.size(), (bloque + 1) * MAX_DESTINATARIOS));
            // El primer bloque usa el id tal cual: un aviso de hasta 200 correos (el caso normal) lleva la clave del contrato.
            final String clave = bloque == 0 ? notificacion.id().toString() : notificacion.id() + "-" + (bloque + 1);
            registrar(notificacion, clave, parte, datos);
        }
        LOGGER.info("Notificacion entregada al servicio de notificaciones: idNotificacion={}, tipo={}, destinatarios={}, outcome=accepted",
                notificacion.id(), notificacion.tipo(), correos.size());
    }

    private void registrar(
            final NotificacionPersistida notificacion,
            final String clave,
            final List<String> correos,
            final Map<String, String> datos
    ) throws NotificacionNoEntregableException {
        final ObjectNode cuerpo = JsonNodeFactory.instance.objectNode();
        cuerpo.put("aplicacion", properties.aplicacion());
        cuerpo.put("idempotencyKey", clave);
        cuerpo.put("tipo", notificacion.tipo());
        cuerpo.put("idioma", properties.idioma());
        if (notificacion.asunto() != null && !notificacion.asunto().isBlank()) {
            cuerpo.put("asunto", recortar(notificacion.asunto().strip(), MAX_ASUNTO));
        }
        if (notificacion.mensaje() != null && !notificacion.mensaje().isBlank()) {
            cuerpo.put("mensaje", recortar(notificacion.mensaje().strip(), MAX_MENSAJE));
        }
        final ArrayNode destinatarios = cuerpo.putArray("destinatarios");
        correos.forEach(correo -> destinatarios.addObject().put("correo", correo));
        final ObjectNode nodoDatos = cuerpo.putObject("datos");
        datos.forEach(nodoDatos::put);
        final UUID correlacion = CorrelationIdContext.get();
        if (correlacion != null) {
            cuerpo.put("idCorrelacion", correlacion.toString());
        }

        try {
            final RespuestaServicio respuesta = cliente.post(RUTA, cuerpo);
            (respuesta.status() == 200 ? duplicadas : aceptadas).increment();
        } catch (final ServicioNotificacionesException e) {
            tratarRechazo(notificacion, e);
        } catch (final ServicioNotificacionesNoDisponibleException e) {
            reintentables.increment();
            LOGGER.warn("Servicio de notificaciones no disponible: idNotificacion={}, tipo={}, outcome=retry, causa={}",
                    notificacion.id(), notificacion.tipo(), e.getCause() != null ? e.getCause().getClass().getSimpleName() : "breaker");
            throw e;
        }
    }

    private void tratarRechazo(final NotificacionPersistida notificacion, final ServicioNotificacionesException e)
            throws NotificacionNoEntregableException {
        final int status = e.status();
        if (status == 401 || status == 403) {
            errorConfiguracion.increment();
            LOGGER.error("El servicio de notificaciones rechazo las credenciales (HTTP {}, code={}): revise NOTIFICACIONES_API_KEY. "
                    + "idNotificacion={}, tipo={}, outcome=config_error", status, e.code(), notificacion.id(), notificacion.tipo());
            throw e;
        }
        if (status >= 500 || status == 408 || status == 429) {
            reintentables.increment();
            LOGGER.warn("Fallo transitorio del servicio de notificaciones: idNotificacion={}, tipo={}, status={}, outcome=retry",
                    notificacion.id(), notificacion.tipo(), status);
            throw e;
        }
        definitivas.increment();
        LOGGER.warn("Aviso rechazado por el servicio de notificaciones: idNotificacion={}, tipo={}, status={}, code={}, outcome=permanent",
                notificacion.id(), notificacion.tipo(), status, e.code());
        throw new NotificacionNoEntregableException(
                "El servicio de notificaciones rechazo el aviso (HTTP " + status + ", " + (e.code().isBlank() ? "SIN_CODIGO" : e.code()) + ").");
    }

    private Map<String, String> combinarDatos(final NotificacionPersistida notificacion, final List<UUID> idsUsuarios)
            throws NotificacionNoEntregableException {
        final Map<String, String> combinados = new LinkedHashMap<>(notificacion.datos());
        final Map<String, String> dinamicos = marcadoresDinamicos.resolver(notificacion.id(), notificacion.tipo(), idsUsuarios);
        if (dinamicos != null) {
            combinados.putAll(dinamicos);
        }
        final Map<String, String> validos = new LinkedHashMap<>();
        for (final Map.Entry<String, String> entrada : combinados.entrySet()) {
            if (entrada.getValue() != null && CLAVE_DATO.matcher(entrada.getKey()).matches() && validos.size() < MAX_DATOS) {
                validos.put(entrada.getKey(), recortar(entrada.getValue(), MAX_VALOR_DATO));
            }
        }
        return validos;
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

    private List<String> correosValidos(final List<ContactoDestinatario> contactos, final boolean exentoDeConfirmacion) {
        final Set<String> validos = new LinkedHashSet<>();
        for (final ContactoDestinatario contacto : contactos == null ? List.<ContactoDestinatario>of() : contactos) {
            if (contacto != null
                    && (exentoDeConfirmacion || !properties.requireConfirmedEmail() || contacto.correoConfirmado())
                    && CorreoSeguro.esValido(contacto.correo())) {
                validos.add(contacto.correo().strip());
            }
        }
        return new ArrayList<>(validos);
    }

    private static String recortar(final String texto, final int maximo) {
        return texto.length() > maximo ? texto.substring(0, maximo) : texto;
    }
}
