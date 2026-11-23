package co.edu.uco.notificaciones.application.service;

import co.edu.uco.notificaciones.application.model.ConfiguracionCanal;
import co.edu.uco.notificaciones.application.port.out.CorreoTransportPort;
import co.edu.uco.notificaciones.application.port.out.MetricasPort;
import co.edu.uco.notificaciones.application.port.out.PlantillaRepositoryPort;
import co.edu.uco.notificaciones.domain.ContenidoRenderizado;
import co.edu.uco.notificaciones.domain.Correos;
import co.edu.uco.notificaciones.domain.CorreoSaliente;
import co.edu.uco.notificaciones.domain.Destinatario;
import co.edu.uco.notificaciones.domain.NoEntregableException;
import co.edu.uco.notificaciones.domain.Notificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Entrega una notificacion por correo: valida el tipo, filtra destinatarios, renderiza, aplica el modo de pruebas (redirige
 * todo a los buzones de prueba), agrupa en BCC y respeta el tope de envios. Nunca registra correos, asunto ni cuerpo en logs.
 */
public class EnviadorCorreo {

    /** Tope del prefijo "[PRUEBA para ...] " para que el asunto original siga visible. */
    public static final int MAX_PREFIJO_ASUNTO_PRUEBA = 120;

    private static final Logger LOGGER = LoggerFactory.getLogger(EnviadorCorreo.class);
    private static final int MAX_SUBJECT_LENGTH = 200;
    private static final String PREFIJO_ASUNTO_PRUEBA = "[PRUEBA para ";
    private static final String CIERRE_PREFIJO_ASUNTO_PRUEBA = "] ";
    private static final String SEPARADOR = ", ";
    private static final String METRICA_ENVIO = "notification.email.send.total";
    private static final String METRICA_DURACION = "notification.email.send.duration";
    private static final String METRICA_DESTINATARIOS = "notification.email.recipients.total";
    private static final String OUTCOME = "outcome";

    private final PlantillaRepositoryPort plantillas;
    private final RenderizadorPlantillas renderizador;
    private final CorreoTransportPort transporte;
    private final TopeEnvios tope;
    private final ConfiguracionCanal configuracion;
    private final MetricasPort metricas;

    public EnviadorCorreo(
            final PlantillaRepositoryPort plantillas,
            final RenderizadorPlantillas renderizador,
            final CorreoTransportPort transporte,
            final TopeEnvios tope,
            final ConfiguracionCanal configuracion,
            final MetricasPort metricas
    ) {
        this.plantillas = Objects.requireNonNull(plantillas, "PlantillaRepositoryPort es obligatorio.");
        this.renderizador = Objects.requireNonNull(renderizador, "RenderizadorPlantillas es obligatorio.");
        this.transporte = Objects.requireNonNull(transporte, "CorreoTransportPort es obligatorio.");
        this.tope = Objects.requireNonNull(tope, "TopeEnvios es obligatorio.");
        this.configuracion = Objects.requireNonNull(configuracion, "ConfiguracionCanal es obligatoria.");
        this.metricas = Objects.requireNonNull(metricas, "MetricasPort es obligatorio.");
        if (configuracion.testModeEnabled()) {
            LOGGER.warn("Modo de pruebas de correo ACTIVO: los correos se redirigen ({} buzones de prueba configurados).",
                    configuracion.testModeRedirectTo().size());
        }
    }

    /** El canal puede tomar trabajo: transporte configurado y cupo de envios en ambas ventanas. */
    public boolean disponible() {
        return transporte.activo() && tope.disponible();
    }

    public void enviar(final Notificacion notificacion) {
        Objects.requireNonNull(notificacion, "La notificacion es obligatoria.");
        if (plantillas.buscarTipo(notificacion.tipo()).isEmpty()) {
            // Un tipo retirado que el administrador reencole no se entrega con el asunto guardado: fallo definitivo.
            LOGGER.warn("Notificacion de tipo no entregable: idNotificacion={}, tipo={}, outcome=permanent",
                    notificacion.id(), notificacion.tipo());
            throw new NoEntregableException("El tipo de notificacion no esta entre los tipos entregables.");
        }

        final List<String> correosValidos = filtrarCorreosValidos(notificacion.destinatarios());
        final int omitidos = Math.max(0, notificacion.destinatarios().size() - correosValidos.size());
        if (omitidos > 0) {
            metricas.contar(METRICA_DESTINATARIOS, omitidos, OUTCOME, "omitted");
        }
        if (correosValidos.isEmpty()) {
            metricas.contar(METRICA_ENVIO, 1, OUTCOME, "no_recipients");
            LOGGER.warn("Notificacion sin destinatarios con correo valido: idNotificacion={}, tipo={}, outcome=no_recipients",
                    notificacion.id(), notificacion.tipo());
            throw new NoEntregableException("Ningun destinatario tiene un correo electronico valido.");
        }

        final boolean modoPrueba = configuracion.testModeEnabled();
        final ContenidoRenderizado contenido;
        final String asunto;
        final List<String> destinatariosEfectivos;
        if (modoPrueba) {
            final String aviso = "MODO DE PRUEBAS: este correo no se envió a los destinatarios reales. "
                    + "Destinatarios originales (" + correosValidos.size() + "): " + String.join(SEPARADOR, correosValidos) + ".";
            contenido = renderizador.renderizar(notificacion.tipo(), notificacion.idioma(), notificacion.datos(),
                    notificacion.asunto(), notificacion.mensaje(), aviso);
            asunto = sanearAsunto(prefijoAsuntoPrueba(correosValidos) + sanearAsunto(contenido.asunto()));
            destinatariosEfectivos = configuracion.testModeRedirectTo();
        } else {
            contenido = renderizador.renderizar(notificacion.tipo(), notificacion.idioma(), notificacion.datos(),
                    notificacion.asunto(), notificacion.mensaje(), null);
            asunto = sanearAsunto(contenido.asunto());
            destinatariosEfectivos = correosValidos;
        }
        if (asunto.isBlank()) {
            throw new NoEntregableException("No hay plantilla ni asunto para entregar la notificacion.");
        }

        final List<List<String>> bloques = partirEnBloques(destinatariosEfectivos, configuracion.maxBcc());
        for (int idx = 0; idx < bloques.size(); idx++) {
            if (!tope.intentarConsumir()) {
                metricas.contar(METRICA_ENVIO, 1, OUTCOME, "retry");
                throw new IllegalStateException("Tope de envios SMTP agotado antes de enviar bloque.");
            }
            enviarBloque(notificacion, contenido, asunto, bloques.get(idx), idx, modoPrueba);
        }
        LOGGER.info("Notificacion enviada: idNotificacion={}, tipo={}, destinatarios={}, origenPlantilla={}, outcome=sent",
                notificacion.id(), notificacion.tipo(), correosValidos.size(), contenido.origen());
    }

    private void enviarBloque(final Notificacion n, final ContenidoRenderizado contenido, final String asunto,
                              final List<String> bloque, final int idx, final boolean modoPrueba) {
        final CorreoSaliente correo = new CorreoSaliente(
                configuracion.fromAddress(), configuracion.fromName(), configuracion.fromAddress(), bloque, asunto,
                contenido.textoPlano(), contenido.html(), "<notif-" + n.id() + "-" + idx + "@uco.edu.co>");
        final long inicio = System.nanoTime();
        try {
            transporte.enviar(correo);
            metricas.medir(METRICA_DURACION, System.nanoTime() - inicio, OUTCOME, "sent");
            metricas.contar(METRICA_ENVIO, 1, OUTCOME, "sent");
            metricas.contar(METRICA_DESTINATARIOS, bloque.size(), OUTCOME, "sent");
            if (modoPrueba) {
                metricas.contar("notification.email.redirected.total", 1);
            }
        } catch (final NoEntregableException ex) {
            metricas.medir(METRICA_DURACION, System.nanoTime() - inicio, OUTCOME, "permanent");
            metricas.contar(METRICA_ENVIO, 1, OUTCOME, "permanent");
            LOGGER.warn("Fallo permanente al enviar notificacion: idNotificacion={}, tipo={}, outcome=permanent", n.id(), n.tipo());
            throw ex;
        } catch (final RuntimeException ex) {
            metricas.medir(METRICA_DURACION, System.nanoTime() - inicio, OUTCOME, "retry");
            metricas.contar(METRICA_ENVIO, 1, OUTCOME, "retry");
            LOGGER.warn("Fallo transitorio al enviar notificacion: idNotificacion={}, tipo={}, outcome=retry, causa={}",
                    n.id(), n.tipo(), ex.getCause() != null ? ex.getCause().getClass().getName() : ex.getClass().getName());
            throw ex;
        }
    }

    private static List<String> filtrarCorreosValidos(final List<Destinatario> destinatarios) {
        final Set<String> validos = new LinkedHashSet<>();
        for (final Destinatario d : destinatarios) {
            if (d != null && Correos.esCorreoSeguro(d.correo())) {
                validos.add(d.correo().trim());
            }
        }
        return List.copyOf(validos);
    }

    /**
     * Prefijo del asunto del modo de pruebas con los destinatarios que habrian recibido el correo: siempre el primero y luego
     * los que quepan sin pasar de {@link #MAX_PREFIJO_ASUNTO_PRUEBA}; el resto se resume como " y M más".
     */
    public static String prefijoAsuntoPrueba(final List<String> originales) {
        final StringBuilder lista = new StringBuilder(originales.getFirst());
        int mostrados = 1;
        for (int idx = 1; idx < originales.size(); idx++) {
            final String candidata = lista + SEPARADOR + originales.get(idx);
            final int restantes = originales.size() - (idx + 1);
            if (componerPrefijo(candidata, restantes).length() > MAX_PREFIJO_ASUNTO_PRUEBA) {
                break;
            }
            lista.append(SEPARADOR).append(originales.get(idx));
            mostrados++;
        }
        return componerPrefijo(lista.toString(), originales.size() - mostrados);
    }

    private static String componerPrefijo(final String lista, final int restantes) {
        final String resumen = restantes > 0 ? " y " + restantes + " más" : "";
        return PREFIJO_ASUNTO_PRUEBA + lista + resumen + CIERRE_PREFIJO_ASUNTO_PRUEBA;
    }

    public static String sanearAsunto(final String asunto) {
        if (asunto == null) {
            return "";
        }
        final String limpio = asunto.replace("\r", " ").replace("\n", " ").trim();
        return limpio.length() > MAX_SUBJECT_LENGTH ? limpio.substring(0, MAX_SUBJECT_LENGTH) : limpio;
    }

    private static List<List<String>> partirEnBloques(final List<String> elementos, final int tamanoMaximo) {
        final List<List<String>> bloques = new ArrayList<>();
        for (int i = 0; i < elementos.size(); i += tamanoMaximo) {
            bloques.add(elementos.subList(i, Math.min(elementos.size(), i + tamanoMaximo)));
        }
        return bloques;
    }
}
