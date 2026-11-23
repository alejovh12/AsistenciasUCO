package co.edu.uco.notificaciones.soporte;

import co.edu.uco.notificaciones.application.model.ConfiguracionCanal;
import co.edu.uco.notificaciones.application.model.ConfiguracionDespachador;
import co.edu.uco.notificaciones.domain.Destinatario;
import co.edu.uco.notificaciones.domain.EstadoEnvio;
import co.edu.uco.notificaciones.domain.Marcador;
import co.edu.uco.notificaciones.domain.Notificacion;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Fabricas de datos de prueba. */
public final class Datos {

    public static final Instant AHORA = Instant.parse("2026-11-20T10:00:00Z");

    private Datos() {
    }

    public static Marcador marcador(final String nombre, final boolean obligatorio) {
        return new Marcador(nombre, "Marcador " + nombre, "ejemplo-" + nombre, obligatorio);
    }

    public static PlantillaRepoFake repoConTipoSaludo() {
        return new PlantillaRepoFake()
                .tipo("SALUDO", false, marcador("nombre", true), marcador("enlace", false), marcador("nota", false))
                .tipo("PRUEBA_CANAL_NOTIFICACION", true)
                .version("SALUDO", 1, "Hola {nombre}", "Hola {nombre}.\n\nAbra {enlace}\nNota: {nota}", true);
    }

    public static Notificacion notificacion(final String tipo, final Map<String, String> datos, final String... correos) {
        return notificacion(UUID.randomUUID(), tipo, datos, 1, correos);
    }

    public static Notificacion notificacion(final UUID id, final String tipo, final Map<String, String> datos, final int intentos, final String... correos) {
        final List<Destinatario> destinatarios = java.util.Arrays.stream(correos).map(c -> new Destinatario(c, "Nombre")).toList();
        return new Notificacion(id, "asistencias", "clave-" + id, tipo, "es", "Asunto guardado", "Mensaje guardado", destinatarios, datos,
                EstadoEnvio.RESERVADA, intentos, AHORA, AHORA, AHORA.plusSeconds(60), "instancia-1", null, null, null, null, "corr-1", null);
    }

    public static ConfiguracionCanal canal(final boolean modoPrueba, final List<String> redirecciones, final int maxBcc) {
        return new ConfiguracionCanal("smtp", "smtp.local", 587, "usuario", true, "noreply@uco.edu.co", "Asistencia UCO", true, false,
                maxBcc, 60, 500, "http://localhost:4200", modoPrueba, redirecciones);
    }

    public static ConfiguracionDespachador despachador(final Duration lease, final int maxIntentos) {
        return new ConfiguracionDespachador(true, 10, lease, Duration.ofSeconds(1), maxIntentos, Duration.ofSeconds(30), Duration.ofMinutes(15),
                30, Duration.ofHours(6), "instancia-1");
    }
}
