package co.edu.uco.notificaciones.application.service;

import co.edu.uco.notificaciones.application.port.out.MetricasPort;
import co.edu.uco.notificaciones.application.port.out.PlantillaRepositoryPort;
import co.edu.uco.notificaciones.domain.ContenidoRenderizado;
import co.edu.uco.notificaciones.domain.Marcador;
import co.edu.uco.notificaciones.domain.VersionPlantilla;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Motor determinista de renderizado: sustituye {@code {marcador}} sin reflexion ni expresiones ejecutables, escapa el HTML
 * y envuelve el resultado en el layout institucional. Orden de resolucion: version activa, version 1 (la de fabrica) y,
 * por ultimo, el asunto y mensaje que envio la aplicacion cliente.
 */
public class RenderizadorPlantillas {

    public static final String ORIGEN_ACTIVA = "activa";
    public static final String ORIGEN_DEFECTO = "defecto";
    public static final String ORIGEN_GUARDADO = "stored";
    public static final String ORIGEN_VISTA_PREVIA = "preview";

    private static final Logger LOGGER = LoggerFactory.getLogger(RenderizadorPlantillas.class);
    private static final String IDIOMA_DEFECTO = "es";
    private static final int VERSION_FABRICA = 1;
    private static final String METRICA_RENDER = "notification.template.render.total";
    private static final int MAX_ASUNTO = 200;
    private static final String MARCADOR_ENLACE = "enlace";
    private static final Pattern TOKEN = Pattern.compile("\\{([a-zA-Z]\\w*)}");
    private static final String PIE_INSTITUCIONAL = "Universidad Católica de Oriente — Sistema de Gestión de Asistencia";

    private final PlantillaRepositoryPort repositorio;
    private final MetricasPort metricas;
    private final String platformUrl;
    private final String layoutHtml;

    public RenderizadorPlantillas(
            final PlantillaRepositoryPort repositorio,
            final MetricasPort metricas,
            final String platformUrl,
            final String layoutHtml
    ) {
        this.repositorio = Objects.requireNonNull(repositorio, "PlantillaRepositoryPort es obligatorio.");
        this.metricas = Objects.requireNonNull(metricas, "MetricasPort es obligatorio.");
        this.platformUrl = platformUrl == null ? "" : platformUrl.trim();
        this.layoutHtml = Objects.requireNonNull(layoutHtml, "El layout es obligatorio.");
    }

    public ContenidoRenderizado renderizar(
            final String tipo,
            final String idioma,
            final Map<String, String> datos,
            final String asuntoGuardado,
            final String mensajeGuardado,
            final String avisoModoPrueba
    ) {
        final Map<String, String> mapaDatos = datos == null ? Map.of() : datos;
        final String tipoNorm = tipo == null ? "" : tipo.trim();
        final String idiomaNorm = (idioma == null || idioma.isBlank()) ? IDIOMA_DEFECTO : idioma.trim();
        final String aviso = (avisoModoPrueba == null || avisoModoPrueba.isBlank()) ? "" : avisoModoPrueba.trim();

        final List<Marcador> marcadores = consultarMarcadoresSeguro(tipoNorm);
        final Optional<VersionPlantilla> activa = consultarSeguro(() -> repositorio.activa(tipoNorm, idiomaNorm), tipoNorm);
        if (activa.isPresent() && esValida(activa.get(), marcadores, mapaDatos)) {
            return construir(activa.get(), mapaDatos, ORIGEN_ACTIVA, aviso);
        }
        final Optional<VersionPlantilla> fabrica = activa.isPresent() && activa.get().version() == VERSION_FABRICA
                ? Optional.empty()
                : consultarSeguro(() -> repositorio.version(tipoNorm, idiomaNorm, VERSION_FABRICA), tipoNorm);
        if (fabrica.isPresent() && esValida(fabrica.get(), marcadores, mapaDatos)) {
            return construir(fabrica.get(), mapaDatos, ORIGEN_DEFECTO, aviso);
        }

        final String asuntoRaw = asuntoGuardado == null ? "" : asuntoGuardado;
        final String cuerpoRaw = mensajeGuardado == null ? "" : mensajeGuardado;
        metricas.contar(METRICA_RENDER, 1, "origin", ORIGEN_GUARDADO);
        return new ContenidoRenderizado(
                sanitizarAsunto(asuntoRaw),
                construirTextoPlano(cuerpoRaw, aviso),
                envolverEnLayout(antecederAvisoHtml(convertirLiteralAHtml(cuerpoRaw), aviso)),
                ORIGEN_GUARDADO);
    }

    public ContenidoRenderizado renderizarExplicito(final String asunto, final String cuerpo, final Map<String, String> datos) {
        final Map<String, String> mapaDatos = datos == null ? Map.of() : datos;
        final String cuerpoNorm = cuerpo == null ? "" : cuerpo;
        return new ContenidoRenderizado(
                sanitizarAsunto(sustituirTextoPlano(asunto == null ? "" : asunto, mapaDatos)),
                construirTextoPlano(sustituirTextoPlano(cuerpoNorm, mapaDatos), ""),
                envolverEnLayout(convertirPlantillaAHtml(cuerpoNorm, mapaDatos)),
                ORIGEN_VISTA_PREVIA);
    }

    private List<Marcador> consultarMarcadoresSeguro(final String tipo) {
        if (tipo.isEmpty()) {
            return List.of();
        }
        try {
            return repositorio.marcadores(tipo);
        } catch (final RuntimeException ex) {
            LOGGER.warn("No fue posible consultar los marcadores del tipo={}: {}", tipo, ex.getClass().getSimpleName());
            return List.of();
        }
    }

    private Optional<VersionPlantilla> consultarSeguro(final java.util.function.Supplier<Optional<VersionPlantilla>> consulta, final String tipo) {
        if (tipo.isEmpty()) {
            return Optional.empty();
        }
        try {
            return consulta.get();
        } catch (final RuntimeException ex) {
            LOGGER.warn("No fue posible consultar la plantilla del tipo={}: {}", tipo, ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private static boolean esValida(final VersionPlantilla plantilla, final List<Marcador> marcadores, final Map<String, String> datos) {
        final Set<String> permitidos = new HashSet<>();
        for (final Marcador marcador : marcadores) {
            permitidos.add(marcador.marcador());
            if (marcador.obligatorio()) {
                final String valor = datos.get(marcador.marcador());
                if (valor == null || valor.isBlank()) {
                    return false;
                }
            }
        }
        return todosDeclarados(plantilla.asunto(), permitidos) && todosDeclarados(plantilla.cuerpo(), permitidos);
    }

    private static boolean todosDeclarados(final String texto, final Set<String> permitidos) {
        final Matcher matcher = TOKEN.matcher(texto);
        while (matcher.find()) {
            if (!permitidos.contains(matcher.group(1))) {
                return false;
            }
        }
        return true;
    }

    private ContenidoRenderizado construir(final VersionPlantilla plantilla, final Map<String, String> datos, final String origen, final String aviso) {
        final String asunto = sanitizarAsunto(sustituirTextoPlano(plantilla.asunto(), datos));
        final String textoPlano = construirTextoPlano(sustituirTextoPlano(plantilla.cuerpo(), datos), aviso);
        final String html = envolverEnLayout(antecederAvisoHtml(convertirPlantillaAHtml(plantilla.cuerpo(), datos), aviso));
        metricas.contar(METRICA_RENDER, 1, "origin", origen);
        return new ContenidoRenderizado(asunto, textoPlano, html, origen);
    }

    private static String sustituirTextoPlano(final String plantilla, final Map<String, String> datos) {
        final Matcher matcher = TOKEN.matcher(plantilla);
        final StringBuilder sb = new StringBuilder();
        int cursor = 0;
        while (matcher.find()) {
            sb.append(plantilla, cursor, matcher.start());
            final String valor = datos.getOrDefault(matcher.group(1), "");
            sb.append(valor == null ? "" : valor);
            cursor = matcher.end();
        }
        sb.append(plantilla, cursor, plantilla.length());
        return sb.toString();
    }

    private static String sustituirEscapadoHtml(final String linea, final Map<String, String> datos) {
        final Matcher matcher = TOKEN.matcher(linea);
        final StringBuilder sb = new StringBuilder();
        int cursor = 0;
        while (matcher.find()) {
            sb.append(escaparHtml(linea.substring(cursor, matcher.start())));
            sb.append(renderizarValorHtml(matcher.group(1), datos.getOrDefault(matcher.group(1), "")));
            cursor = matcher.end();
        }
        sb.append(escaparHtml(linea.substring(cursor)));
        return sb.toString();
    }

    /** {@code enlace} (valor emitido por el sistema cliente) se muestra como vinculo solo si es http o https; el resto se escapa. */
    private static String renderizarValorHtml(final String nombre, final String valor) {
        final String texto = valor == null ? "" : valor;
        final String escapado = escaparHtml(texto);
        if (MARCADOR_ENLACE.equals(nombre) && esUrlHttp(texto)) {
            return "<a href=\"" + escapado + "\" style=\"color:#0f5132;\">" + escapado + "</a>";
        }
        return escapado;
    }

    private static boolean esUrlHttp(final String texto) {
        final String minusculas = texto.toLowerCase(Locale.ROOT);
        return (minusculas.startsWith("https://") || minusculas.startsWith("http://")) && texto.indexOf(' ') < 0;
    }

    private static String convertirPlantillaAHtml(final String cuerpo, final Map<String, String> datos) {
        return aParrafos(cuerpo, linea -> sustituirEscapadoHtml(linea, datos));
    }

    private static String convertirLiteralAHtml(final String cuerpo) {
        return aParrafos(cuerpo, RenderizadorPlantillas::escaparHtml);
    }

    private static String aParrafos(final String cuerpo, final java.util.function.UnaryOperator<String> porLinea) {
        final String normalizado = cuerpo.replace("\r\n", "\n").replace("\r", "\n");
        final StringBuilder sb = new StringBuilder();
        for (final String parrafo : normalizado.split("\n\\s*\n", -1)) {
            final String[] lineas = parrafo.split("\n", -1);
            sb.append("<p>");
            for (int i = 0; i < lineas.length; i++) {
                if (i > 0) {
                    sb.append("<br>");
                }
                sb.append(porLinea.apply(lineas[i]));
            }
            sb.append("</p>");
        }
        return sb.toString();
    }

    private String construirTextoPlano(final String cuerpo, final String aviso) {
        final String conAviso = aviso.isEmpty() ? cuerpo : aviso + "\n\n" + cuerpo;
        final String pie = "\n\n---\n" + PIE_INSTITUCIONAL;
        return platformUrl.isEmpty() ? conAviso + pie : conAviso + pie + "\n" + platformUrl;
    }

    private static String antecederAvisoHtml(final String cuerpoHtml, final String aviso) {
        return aviso.isEmpty() ? cuerpoHtml : "<p>" + escaparHtml(aviso) + "</p>" + cuerpoHtml;
    }

    private String envolverEnLayout(final String contenidoHtml) {
        final String pie;
        if (platformUrl.isEmpty()) {
            pie = "Mensaje automático del Sistema de Gestión de Asistencia — Universidad Católica de Oriente.";
        } else {
            pie = "Mensaje automático del Sistema de Gestión de Asistencia — "
                    + "<a href=\"" + escaparHtml(platformUrl) + "\" style=\"color:#0f5132;\">Ir a la plataforma</a>.";
        }
        return layoutHtml.replace("{{pie}}", pie).replace("{{contenido}}", contenidoHtml);
    }

    private static String sanitizarAsunto(final String asunto) {
        final String limpio = asunto.replace("\r\n", " ").replace('\r', ' ').replace('\n', ' ').trim();
        return limpio.length() > MAX_ASUNTO ? limpio.substring(0, MAX_ASUNTO) : limpio;
    }

    public static String escaparHtml(final String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        final StringBuilder sb = new StringBuilder(input.length() + 16);
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
