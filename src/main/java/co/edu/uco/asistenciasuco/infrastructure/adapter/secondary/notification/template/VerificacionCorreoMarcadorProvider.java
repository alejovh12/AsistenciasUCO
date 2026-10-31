package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.crosscutting.util.TokenHashHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionNoEntregableException;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * IDENT-001: emite el enlace de verificacion de correo <b>al entregar</b> el aviso. El token (32 bytes de
 * {@link SecureRandom} en base64url) nunca se persiste ni se registra en logs: solo su SHA-256 llega a la base de datos.
 * El enlace usa el fragmento {@code #token=} para que el token no viaje en la peticion HTTP de la pagina ni en
 * {@code Referer}. Un correo ya verificado o un usuario inexistente es un fallo permanente; cualquier otro, reintentable.
 */
public final class VerificacionCorreoMarcadorProvider implements MarcadorDinamicoProvider {

    static final String RUTA_VERIFICACION = "/verificar-correo#token=";
    static final String MARCADOR_ENLACE = "enlace";
    private static final int BYTES_TOKEN = 32;

    private final TokenVerificacionCorreoRepository repositorio;
    private final String platformUrl;
    private final SecureRandom random;

    public VerificacionCorreoMarcadorProvider(
            final TokenVerificacionCorreoRepository repositorio,
            final String platformUrl,
            final SecureRandom random
    ) {
        this.repositorio = Objects.requireNonNull(repositorio, "TokenVerificacionCorreoRepository es obligatorio.");
        this.random = Objects.requireNonNull(random, "SecureRandom es obligatorio.");
        this.platformUrl = platformUrl == null ? "" : platformUrl.strip();
    }

    @Override
    public Map<String, String> resolver(final String tipo, final List<UUID> idsUsuarios)
            throws NotificacionNoEntregableException {
        if (!Notificacion.TIPO_VERIFICACION_CORREO.equals(tipo)) {
            return Map.of();
        }
        if (idsUsuarios == null || idsUsuarios.size() != 1) {
            throw new NotificacionNoEntregableException(
                    "El aviso de verificacion de correo exige exactamente un destinatario.");
        }
        if (platformUrl.isEmpty()) {
            throw new NotificacionNoEntregableException(
                    "No hay URL de plataforma configurada para armar el enlace de verificacion.");
        }

        final byte[] aleatorio = new byte[BYTES_TOKEN];
        random.nextBytes(aleatorio);
        final String token = Base64.getUrlEncoder().withoutPadding().encodeToString(aleatorio);

        try {
            repositorio.registrar(idsUsuarios.getFirst(), TokenHashHelper.sha256(token));
        } catch (final ConflictException | ResourceNotFoundException exception) {
            throw new NotificacionNoEntregableException(
                    "El usuario ya verifico su correo o no esta disponible para verificarlo.", exception);
        }
        return Map.of(MARCADOR_ENLACE, baseSinBarraFinal() + RUTA_VERIFICACION + token);
    }

    private String baseSinBarraFinal() {
        return platformUrl.endsWith("/") ? platformUrl.substring(0, platformUrl.length() - 1) : platformUrl;
    }
}
