package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.crosscutting.util.TokenHashHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionNoEntregableException;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * IDENT-001: emite el enlace de verificacion de correo <b>al entregar</b> el aviso. El token (32 bytes de
 * {@link SecureRandom} en base64url) nunca se persiste ni se registra en logs: solo su SHA-256 llega a la base de datos.
 * El enlace usa el fragmento {@code #token=} para que el token no viaje en la peticion HTTP de la pagina ni en
 * {@code Referer}. Un correo ya verificado o un usuario inexistente es un fallo permanente; cualquier otro, reintentable.
 *
 * <p>HU177: el mismo mecanismo emite el enlace para definir una contrasena nueva. Cada tipo de aviso con enlace de un
 * solo uso declara su ruta en la plataforma y el repositorio que registra el hash ({@link #conEnlace}).</p>
 */
public final class VerificacionCorreoMarcadorProvider implements MarcadorDinamicoProvider {

    static final String RUTA_VERIFICACION = "/verificar-correo#token=";
    /** HU177: pagina publica donde el usuario define su contrasena nueva. */
    public static final String RUTA_RECUPERACION_CLAVE = "/restablecer-clave#token=";
    static final String MARCADOR_ENLACE = "enlace";
    private static final int BYTES_TOKEN = 32;

    private final Map<String, Enlace> enlaces;
    private final String platformUrl;
    private final SecureRandom random;

    public VerificacionCorreoMarcadorProvider(
            final TokenVerificacionCorreoRepository repositorio,
            final String platformUrl,
            final SecureRandom random
    ) {
        this(
                Map.of(Notificacion.TIPO_VERIFICACION_CORREO, new Enlace(
                        RUTA_VERIFICACION,
                        Objects.requireNonNull(repositorio, "TokenVerificacionCorreoRepository es obligatorio."),
                        "El usuario ya verifico su correo o no esta disponible para verificarlo.")),
                platformUrl,
                random
        );
    }

    private VerificacionCorreoMarcadorProvider(
            final Map<String, Enlace> enlaces,
            final String platformUrl,
            final SecureRandom random
    ) {
        this.enlaces = Map.copyOf(enlaces);
        this.random = Objects.requireNonNull(random, "SecureRandom es obligatorio.");
        this.platformUrl = platformUrl == null ? "" : platformUrl.strip();
    }

    /**
     * Devuelve un proveedor que ademas emite el enlace de un solo uso de otro tipo de aviso.
     *
     * @param tipo        tipo de notificacion que lleva el marcador {@code enlace}.
     * @param ruta        ruta de la plataforma que recibe el token en el fragmento (termina en {@code #token=}).
     * @param repositorio registra el SHA-256 del token recien emitido.
     * @param rechazo     motivo del fallo permanente cuando la base de datos rechaza la emision.
     */
    public VerificacionCorreoMarcadorProvider conEnlace(
            final String tipo,
            final String ruta,
            final TokenVerificacionCorreoRepository repositorio,
            final String rechazo
    ) {
        final Map<String, Enlace> ampliado = new LinkedHashMap<>(enlaces);
        ampliado.put(
                Objects.requireNonNull(tipo, "El tipo de notificacion es obligatorio."),
                new Enlace(
                        Objects.requireNonNull(ruta, "La ruta del enlace es obligatoria."),
                        Objects.requireNonNull(repositorio, "El repositorio del token es obligatorio."),
                        Objects.requireNonNull(rechazo, "El motivo de rechazo es obligatorio.")));
        return new VerificacionCorreoMarcadorProvider(ampliado, platformUrl, random);
    }

    @Override
    public Map<String, String> resolver(final String tipo, final List<UUID> idsUsuarios)
            throws NotificacionNoEntregableException {
        final Enlace enlace = tipo == null ? null : enlaces.get(tipo);
        if (enlace == null) {
            return Map.of();
        }
        if (idsUsuarios == null || idsUsuarios.size() != 1) {
            throw new NotificacionNoEntregableException(
                    "El aviso con enlace de un solo uso exige exactamente un destinatario.");
        }
        if (platformUrl.isEmpty()) {
            throw new NotificacionNoEntregableException(
                    "No hay URL de plataforma configurada para armar el enlace de un solo uso.");
        }

        final byte[] aleatorio = new byte[BYTES_TOKEN];
        random.nextBytes(aleatorio);
        final String token = Base64.getUrlEncoder().withoutPadding().encodeToString(aleatorio);
        try {
            enlace.repositorio().registrar(idsUsuarios.getFirst(), TokenHashHelper.sha256(token));
        } catch (final ConflictException | ResourceNotFoundException exception) {
            throw new NotificacionNoEntregableException(enlace.rechazo(), exception);
        }
        return Map.of(MARCADOR_ENLACE, baseSinBarraFinal() + enlace.ruta() + token);
    }

    private String baseSinBarraFinal() {
        return platformUrl.endsWith("/") ? platformUrl.substring(0, platformUrl.length() - 1) : platformUrl;
    }

    /** Ruta de la plataforma, registro del hash y motivo del rechazo permanente de un tipo de aviso con enlace. */
    private record Enlace(String ruta, TokenVerificacionCorreoRepository repositorio, String rechazo) {
    }
}
