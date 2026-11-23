package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response;

import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.primaryports.ResolverMensajeUsuarioInputPort;

import java.text.MessageFormat;
import java.util.Optional;
import java.util.regex.Matcher;

/**
 * Resuelve las confirmaciones de {@link MensajesExitoHttp} por el catalogo de mensajes (puerto de entrada, nunca el secundario) y
 * cae al texto de respaldo cuando no hay catalogo o no tiene el codigo. Los marcadores se rellenan con la misma convencion que
 * el catalogo: {@code {}} secuencial o {@code {0}} posicional ({@link MessageFormat}); los argumentos se pasan como texto para
 * que un entero no salga con separador de miles.
 */
public final class MensajesExito {

    private static final MensajesExito SOLO_RESPALDO = new MensajesExito(null);

    private final ResolverMensajeUsuarioInputPort resolver;

    public MensajesExito(final ResolverMensajeUsuarioInputPort resolver) {
        this.resolver = resolver;
    }

    public static MensajesExito soloRespaldo() {
        return SOLO_RESPALDO;
    }

    public String de(final MensajesExitoHttp mensaje, final Object... argumentos) {
        final Optional<String> delCatalogo = resolver == null ? Optional.empty() : resolver.execute(mensaje.codigo());
        return formatear(delCatalogo.filter(texto -> !texto.isBlank()).orElse(mensaje.respaldo()), argumentos);
    }

    private static String formatear(final String plantilla, final Object... argumentos) {
        if (argumentos == null || argumentos.length == 0) {
            return plantilla;
        }
        final Object[] comoTexto = new Object[argumentos.length];
        for (int i = 0; i < argumentos.length; i++) {
            comoTexto[i] = String.valueOf(argumentos[i]);
        }
        if (plantilla.contains("{}")) {
            String resultado = plantilla;
            for (final Object argumento : comoTexto) {
                resultado = resultado.replaceFirst("\\{}", Matcher.quoteReplacement((String) argumento));
            }
            return resultado;
        }
        try {
            return MessageFormat.format(plantilla, comoTexto);
        } catch (final IllegalArgumentException _) {
            return plantilla;
        }
    }
}
