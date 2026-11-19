package co.edu.uco.asistenciasuco.application.features.tomaasistencia.common;

import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * CULM-001 B.2 (contrato §3.8): publica los eventos SSE de las tomas despues del commit del SP. La publicacion es de mejor
 * esfuerzo: un fallo del canal se registra y no cambia la respuesta (el comando ya quedo confirmado). Los payloads
 * llevan {@code grupo} (filtro del canal) y nunca datos personales.
 */
public final class TomaAsistenciaEventos {

    public static final String TOMA_ABIERTA = "TOMA_ABIERTA";
    public static final String TOMA_CERRADA = "TOMA_CERRADA";
    public static final String ASISTENCIAS_TOMA_ACTUALIZADAS = "ASISTENCIAS_TOMA_ACTUALIZADAS";
    private static final Logger LOGGER = LoggerFactory.getLogger(TomaAsistenciaEventos.class);

    private final RealtimePublisherPort realtime;

    public TomaAsistenciaEventos(final RealtimePublisherPort realtime) {
        this.realtime = Objects.requireNonNull(realtime, "RealtimePublisherPort es obligatorio.");
    }

    /**
     * Publica {@code TOMA_CERRADA}. {@code totalFaltantes} nulo se omite (cierre automatico y cierre por nueva toma, donde el
     * SP no lo calcula).
     */
    public void tomaCerrada(final UUID grupo, final UUID sesion, final UUID toma, final int numero,
                            final int totalAusentesMarcados, final Integer totalFaltantes, final boolean automatico) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("grupo", grupo.toString());
        payload.put("sesion", sesion.toString());
        payload.put("toma", toma.toString());
        payload.put("numero", numero);
        payload.put("totalAusentesMarcados", totalAusentesMarcados);
        if (totalFaltantes != null) {
            payload.put("totalFaltantes", totalFaltantes);
        }
        payload.put("automatico", automatico);
        publicar(TOMA_CERRADA, payload);
    }

    public void asistenciasTomaActualizadas(final UUID grupo, final UUID sesion, final UUID toma, final int totalRegistros) {
        publicar(ASISTENCIAS_TOMA_ACTUALIZADAS, Map.of("grupo", grupo.toString(), "sesion", sesion.toString(),
                "toma", toma.toString(), "totalRegistros", totalRegistros));
    }

    public void publicar(final String tipo, final Map<String, Object> payload) {
        try {
            realtime.publish(RealtimeEvent.of(tipo, payload));
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible publicar el evento {} del grupo {}: {}", tipo, payload.get("grupo"),
                    exception.getClass().getSimpleName());
        }
    }
}
