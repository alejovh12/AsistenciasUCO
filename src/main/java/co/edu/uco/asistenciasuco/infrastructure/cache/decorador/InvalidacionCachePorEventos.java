package co.edu.uco.asistenciasuco.infrastructure.cache.decorador;

import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.infrastructure.cache.CacheConsultas;
import co.edu.uco.asistenciasuco.infrastructure.cache.RegionCache;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * REDIS-001 (TD-083): mantiene fresco el portal cuando otro usuario cambia los datos de un grupo. Observa los eventos de
 * tiempo real que esta replica entrega (los propios y los relevados por Redis) y borra la cache del portal asociada al
 * grupo del evento: materias, horarios, resumen y plan de cada estudiante del grupo, y asignaturas y horarios del docente.
 *
 * <ul>
 *   <li>{@code grupo} en el payload: se invalida la etiqueta {@code grupo:<id>} (ver {@link ConsultasCacheadasPostProcessor}).</li>
 *   <li>{@code ALERTA_INASISTENCIA}: ademas se invalida el portal del estudiante destinatario.</li>
 * </ul>
 *
 * <p>Nunca falla: la cache degrada por su cuenta y el adaptador realtime aisla al observador.</p>
 */
public final class InvalidacionCachePorEventos implements Consumer<RealtimeEvent> {

    /** Eventos que cambian asistencias, sesiones o el padron de un grupo. */
    static final Set<String> TIPOS_GRUPO = Set.of(
            "ASISTENCIAS_TOMA_ACTUALIZADAS", "ASISTENCIAS_SESION_ACTUALIZADAS", "TOMA_CERRADA", "SESION_CANCELADA",
            "SESION_MANUAL_CREADA", "ESTUDIANTE_MATRICULADO", "FALTA_SESION_AVISADA");
    /** Eventos dirigidos a estudiantes cuyo portal cambia (porcentaje de inasistencia). */
    static final Set<String> TIPOS_ESTUDIANTE = Set.of("ALERTA_INASISTENCIA");

    private final Supplier<CacheConsultas> cache;

    public InvalidacionCachePorEventos(final Supplier<CacheConsultas> cache) {
        this.cache = Objects.requireNonNull(cache, "La cache es obligatoria.");
    }

    @Override
    public void accept(final RealtimeEvent event) {
        if (event == null) {
            return;
        }
        if (TIPOS_GRUPO.contains(event.type())) {
            final UUID grupo = grupo(event);
            if (grupo != null) {
                cache.get().invalidarEtiqueta(RegionCache.PORTAL, ConsultasCacheadasPostProcessor.etiquetaGrupo(grupo));
            }
        }
        if (TIPOS_ESTUDIANTE.contains(event.type())) {
            for (final UUID usuario : event.destinatariosUsuario()) {
                ConsultasCacheadasPostProcessor.invalidarUsuario(cache.get(), usuario,
                        ConsultasCacheadasPostProcessor.PORTAL_ESTUDIANTE);
            }
        }
    }

    private static UUID grupo(final RealtimeEvent event) {
        final Object valor = event.payload().get("grupo");
        if (valor == null) {
            return null;
        }
        try {
            return UUID.fromString(valor.toString());
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }
}
