package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.service;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.EstadoCanalNotificacionProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.MarcadorPlantillaProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.NotificacionSalienteAdminProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.PaginaColaNotificacionProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.ResumenColaNotificacionProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.TipoPlantillaAdminProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.VersionPlantillaProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.VistaPreviaPlantillaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.admin.AdministracionNotificacionPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contact.ContactoDestinatario;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contact.ContactoDestinatarioResolver;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.NotificacionesServiceProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Administracion de plantillas y de la cola de notificaciones sobre el Servicio de Notificaciones UCO: cada metodo del puerto
 * es una llamada HTTP del contrato. Los identificadores de la cola son los de la cola del servicio (no los de la
 * {@code NotificacionSaliente} del outbox del backend, que solo registra «entregado al servicio»). Los errores del servicio se
 * traducen a los codigos que ya conoce el frontend (ver {@link NotificacionesServiceErrores}).
 */
public final class AdministracionNotificacionServicioAdapter implements AdministracionNotificacionPort {

    private static final String BASE = "/api/v1";
    private static final String TIPO_CORREO_GENERICO = null;

    private final NotificacionesServiceClient cliente;
    private final NotificacionesServiceProperties properties;
    private final ContactoDestinatarioResolver contactoResolver;
    private final ObjectMapper objectMapper;

    public AdministracionNotificacionServicioAdapter(
            final NotificacionesServiceClient cliente,
            final NotificacionesServiceProperties properties,
            final ContactoDestinatarioResolver contactoResolver,
            final ObjectMapper objectMapper
    ) {
        this.cliente = Objects.requireNonNull(cliente, "NotificacionesServiceClient es obligatorio.");
        this.properties = Objects.requireNonNull(properties, "NotificacionesServiceProperties es obligatorio.");
        this.contactoResolver = Objects.requireNonNull(contactoResolver, "ContactoDestinatarioResolver es obligatorio.");
        this.objectMapper = Objects.requireNonNull(objectMapper, "El ObjectMapper es obligatorio.");
    }

    @Override
    public EstadoCanalNotificacionProjection consultarEstadoCanal() {
        try {
            final JsonNode estado = cliente.get(BASE + "/estado-canal", Map.of()).cuerpo();
            return new EstadoCanalNotificacionProjection(
                    "servicio",
                    texto(estado, "canal", "servicio"),
                    estado.path("dispatcherEnabled").asBoolean(false),
                    estado.path("disponible").asBoolean(false),
                    texto(estado, "host", ""),
                    estado.path("port").asInt(0),
                    texto(estado, "username", ""),
                    estado.path("tienePasswordConfigurado").asBoolean(false),
                    texto(estado, "fromAddress", ""),
                    texto(estado, "fromName", ""),
                    estado.path("starttls").asBoolean(false),
                    estado.path("ssl").asBoolean(false),
                    estado.path("maxBcc").asInt(0),
                    estado.path("ratePerMinute").asInt(0),
                    estado.path("ratePerDay").asInt(0),
                    properties.requireConfirmedEmail(),
                    texto(estado, "platformUrl", properties.platformUrl()),
                    estado.path("testModeEnabled").asBoolean(false),
                    estado.path("testModeRedirectCount").asInt(0));
        } catch (final ServicioNotificacionesNoDisponibleException | ServicioNotificacionesException e) {
            // El estado del canal responde aunque el servicio este caido: el canal simplemente no esta disponible.
            return new EstadoCanalNotificacionProjection("servicio", "servicio", false, false, "", 0, "", false, "", "",
                    false, false, 0, 0, 0, properties.requireConfirmedEmail(), properties.platformUrl(), false, 0);
        }
    }

    @Override
    public List<TipoPlantillaAdminProjection> consultarPlantillas(final String idioma, final UUID idUsuarioEjecutor) {
        final JsonNode lista = llamar(() -> cliente.get(BASE + "/plantillas", Map.of("idioma", idiomaNormalizado(idioma)))).cuerpo();
        final List<TipoPlantillaAdminProjection> tipos = new ArrayList<>();
        for (final JsonNode nodo : lista) {
            final String tipo = texto(nodo, "tipo", "");
            final String idiomaTipo = texto(nodo, "idioma", idiomaNormalizado(idioma));
            tipos.add(new TipoPlantillaAdminProjection(
                    tipo,
                    idiomaTipo,
                    nodo.path("versionActiva").asInt(0),
                    texto(nodo, "asuntoActivo", ""),
                    texto(nodo, "cuerpoActivo", ""),
                    instante(nodo.path("actualizadoEn")) == null ? Instant.now() : instante(nodo.path("actualizadoEn")),
                    marcadores(nodo.path("marcadores")),
                    versiones(nodo.path("versiones"), tipo, idiomaTipo),
                    texto(nodo, "descripcion", "")));
        }
        return tipos;
    }

    @Override
    public VersionPlantillaProjection crearVersionPlantilla(
            final String tipo, final String idioma, final String asunto, final String cuerpo, final UUID creadoPor) {
        final ObjectNode solicitud = objectMapper.createObjectNode()
                .put("idioma", idiomaNormalizado(idioma))
                .put("asunto", asunto == null ? "" : asunto.strip())
                .put("cuerpo", cuerpo == null ? "" : cuerpo.strip());
        if (creadoPor != null) {
            solicitud.put("creadoPor", creadoPor.toString());
        }
        final JsonNode version = llamar(() -> cliente.post(BASE + "/plantillas/" + segmento(tipo) + "/versiones", solicitud)).cuerpo();
        return version(version, tipo.strip(), idiomaNormalizado(idioma));
    }

    @Override
    public void activarVersionPlantilla(final String tipo, final String idioma, final int version, final UUID activadoPor) {
        llamar(() -> cliente.post(BASE + "/plantillas/" + segmento(tipo) + "/versiones/" + version + "/activacion",
                Map.of("idioma", idiomaNormalizado(idioma)), null));
    }

    @Override
    public VistaPreviaPlantillaProjection generarVistaPrevia(final String tipo, final String idioma, final String asunto, final String cuerpo) {
        final ObjectNode solicitud = objectMapper.createObjectNode()
                .put("tipo", tipo == null ? "" : tipo.strip())
                .put("idioma", idiomaNormalizado(idioma))
                .put("asunto", asunto == null ? "" : asunto)
                .put("cuerpo", cuerpo == null ? "" : cuerpo);
        final JsonNode vista = llamar(() -> cliente.post(BASE + "/plantillas/vista-previa", solicitud)).cuerpo();
        return new VistaPreviaPlantillaProjection(
                texto(vista, "asunto", ""), texto(vista, "textoPlano", ""), texto(vista, "html", ""), texto(vista, "origen", "preview"));
    }

    @Override
    public PaginaColaNotificacionProjection consultarCola(
            final String estado, final String tipo, final int pagina, final int tamano, final UUID idUsuarioEjecutor) {
        final int numeroPagina = Math.max(1, pagina);
        final int tamanoPagina = tamano <= 0 || tamano > 100 ? 20 : tamano;
        final Map<String, String> consulta = new LinkedHashMap<>();
        consulta.put("aplicacion", properties.aplicacion());
        if (estado != null && !estado.isBlank()) {
            consulta.put("estado", estado.strip());
        }
        if (tipo != null && !tipo.isBlank()) {
            consulta.put("tipo", tipo.strip());
        }
        consulta.put("pagina", Integer.toString(numeroPagina));
        consulta.put("tamano", Integer.toString(tamanoPagina));

        final JsonNode cuerpo = llamar(() -> cliente.get(BASE + "/notificaciones", consulta)).cuerpo();
        final List<NotificacionSalienteAdminProjection> elementos = new ArrayList<>();
        for (final JsonNode nodo : cuerpo.path("elementos")) {
            elementos.add(notificacion(nodo));
        }
        final JsonNode resumen = cuerpo.path("resumen");
        return new PaginaColaNotificacionProjection(
                elementos,
                cuerpo.path("numeroPagina").asInt(numeroPagina),
                cuerpo.path("tamanoPagina").asInt(tamanoPagina),
                cuerpo.path("totalElementos").asLong(elementos.size()),
                cuerpo.path("totalPaginas").asInt(1),
                new ResumenColaNotificacionProjection(
                        resumen.path("pendientes").asLong(0),
                        resumen.path("reservadas").asLong(0),
                        resumen.path("entregadas").asLong(0),
                        resumen.path("fallidasReintentables").asLong(0),
                        resumen.path("fallidasDefinitivas").asLong(0)));
    }

    @Override
    public void reintentarNotificacion(final UUID idNotificacion, final UUID usuarioEjecutor) {
        Objects.requireNonNull(idNotificacion, "El identificador de la notificacion es obligatorio.");
        llamar(() -> cliente.post(BASE + "/notificaciones/" + idNotificacion + "/reintento", null));
    }

    @Override
    public void enviarCorreoPrueba(final UUID idUsuarioEjecutor, final String correoDestino) {
        enviarCorreoPrueba(idUsuarioEjecutor, TIPO_CORREO_GENERICO, correoDestino);
    }

    @Override
    public void enviarCorreoPrueba(final UUID idUsuarioEjecutor, final String tipo, final String correoDestino) {
        final String destino = correoDestino != null && !correoDestino.isBlank()
                ? correoDestino.strip()
                : correoDelEjecutor(idUsuarioEjecutor);
        final ObjectNode solicitud = objectMapper.createObjectNode().put("correoDestino", destino);
        if (tipo != null && !tipo.isBlank()) {
            solicitud.put("tipo", tipo.strip());
        }
        if (idUsuarioEjecutor != null) {
            solicitud.put("solicitadoPor", idUsuarioEjecutor.toString());
        }
        llamar(() -> cliente.post(BASE + "/correos-prueba", solicitud));
    }

    /** Sin correo de destino la prueba va al correo del propio administrador, como antes del servicio. */
    private String correoDelEjecutor(final UUID idUsuarioEjecutor) {
        if (idUsuarioEjecutor != null) {
            return contactoResolver.consultarContactos(List.of(idUsuarioEjecutor)).stream()
                    .map(ContactoDestinatario::correo)
                    .filter(correo -> correo != null && !correo.isBlank())
                    .findFirst()
                    .orElseThrow(AdministracionNotificacionServicioAdapter::sinCorreoDeDestino);
        }
        throw sinCorreoDeDestino();
    }

    private static ValidationException sinCorreoDeDestino() {
        return new ValidationException("Indique un correo de destino: el administrador no tiene un correo registrado.");
    }

    private RespuestaServicio llamar(final Supplier<RespuestaServicio> llamada) {
        try {
            return llamada.get();
        } catch (final ServicioNotificacionesException e) {
            throw NotificacionesServiceErrores.traducir(e);
        } catch (final ServicioNotificacionesNoDisponibleException e) {
            throw NotificacionesServiceErrores.noDisponible(e);
        }
    }

    private static String idiomaNormalizado(final String idioma) {
        return idioma == null || idioma.isBlank() ? "es" : idioma.strip();
    }

    private static String segmento(final String tipo) {
        return NotificacionesServiceClient.codificar(Objects.requireNonNull(tipo, "El tipo de plantilla es obligatorio.").strip());
    }

    private static List<MarcadorPlantillaProjection> marcadores(final JsonNode nodos) {
        final List<MarcadorPlantillaProjection> marcadores = new ArrayList<>();
        for (final JsonNode nodo : nodos) {
            marcadores.add(new MarcadorPlantillaProjection(
                    texto(nodo, "marcador", ""), texto(nodo, "descripcion", ""), texto(nodo, "ejemplo", ""),
                    nodo.path("obligatorio").asBoolean(false)));
        }
        return marcadores;
    }

    private static List<VersionPlantillaProjection> versiones(final JsonNode nodos, final String tipo, final String idioma) {
        final List<VersionPlantillaProjection> versiones = new ArrayList<>();
        for (final JsonNode nodo : nodos) {
            versiones.add(version(nodo, tipo, idioma));
        }
        return versiones;
    }

    private static VersionPlantillaProjection version(final JsonNode nodo, final String tipo, final String idioma) {
        final Instant creadoEn = instante(nodo.path("creadoEn"));
        return new VersionPlantillaProjection(
                uuid(nodo.path("id")) == null ? UUID.randomUUID() : uuid(nodo.path("id")),
                texto(nodo, "tipo", tipo),
                texto(nodo, "idioma", idioma),
                nodo.path("version").asInt(0),
                texto(nodo, "asunto", ""),
                texto(nodo, "cuerpo", ""),
                nodo.path("activa").asBoolean(false),
                creadoEn == null ? Instant.now() : creadoEn);
    }

    private static NotificacionSalienteAdminProjection notificacion(final JsonNode nodo) {
        final String codigoEstado = texto(nodo, "codigoEstado", "");
        return new NotificacionSalienteAdminProjection(
                uuid(nodo.path("id")),
                texto(nodo, "tipo", ""),
                texto(nodo, "asunto", ""),
                codigoEstado,
                texto(nodo, "nombreEstado", codigoEstado),
                nodo.path("esTerminal").asBoolean(false),
                nodo.path("cantidadDestinatarios").asInt(0),
                nodo.path("intentos").asInt(0),
                instante(nodo.path("creadoEn")),
                instante(nodo.path("proximoIntentoEn")),
                instante(nodo.path("entregadoEn")),
                textoONulo(nodo, "codigoError"),
                textoONulo(nodo, "detalleError"),
                uuid(nodo.path("idCorrelacion")));
    }

    private static String texto(final JsonNode nodo, final String campo, final String porDefecto) {
        final JsonNode valor = nodo.path(campo);
        return valor.isMissingNode() || valor.isNull() ? porDefecto : valor.asText(porDefecto);
    }

    private static String textoONulo(final JsonNode nodo, final String campo) {
        return texto(nodo, campo, null);
    }

    private static UUID uuid(final JsonNode nodo) {
        if (nodo.isMissingNode() || nodo.isNull()) {
            return null;
        }
        try {
            return UUID.fromString(nodo.asText());
        } catch (final IllegalArgumentException _) {
            return null;
        }
    }

    private static Instant instante(final JsonNode nodo) {
        if (nodo.isMissingNode() || nodo.isNull() || nodo.asText().isBlank()) {
            return null;
        }
        try {
            return Instant.parse(nodo.asText());
        } catch (final DateTimeParseException _) {
            try {
                return OffsetDateTime.parse(nodo.asText()).toInstant();
            } catch (final DateTimeParseException _) {
                return null;
            }
        }
    }
}
