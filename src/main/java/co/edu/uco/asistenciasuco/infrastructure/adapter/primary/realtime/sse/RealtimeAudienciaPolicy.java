package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Politica de audiencia por tipo de evento del canal SSE de grupo (CULM-001 B.1 [SUPUESTO-B-17], extendida en C.1, contrato 3.6.3).
 * Compara el tipo por <b>igualdad exacta</b> (nunca por prefijo), de modo que un tipo {@code topic.action} emitido por
 * {@code POST /realtime/emit} no puede coincidir con un tipo de negocio. Un tipo no listado se entrega solo al docente titular.
 *
 * <p>{@link #entrega} = (audiencias del tipo interseca las del suscriptor) o ({@code DESTINATARIO} esta entre las del tipo y el usuario es
 * destinatario explicito del evento). Los destinatarios son un metadato del evento calculado en la DB; nunca viajan en el payload.</p>
 */
public final class RealtimeAudienciaPolicy {

    /** Audiencias del canal SSE de grupo. */
    public enum Audiencia {
        DOCENTE_TITULAR,
        ESTUDIANTE_GRUPO,
        AMBITO_GRUPO,
        /** Solo los usuarios nombrados como destinatarios del evento (alertas y revisiones). */
        DESTINATARIO,
        /**
         * Eventos de configuracion (p. ej. {@code PARAMETER_UPDATED}, CULM-001 D.3): solo el rol ADMINISTRADOR. Ningun
         * suscriptor del canal de grupo la tiene, de modo que estos tipos no se entregan por {@code /realtime/stream}.
         */
        ADMINISTRADOR
    }

    private static final Set<Audiencia> SOLO_TITULAR = EnumSet.of(Audiencia.DOCENTE_TITULAR);
    private static final Set<Audiencia> GRUPO_COMPLETO = EnumSet.of(Audiencia.DOCENTE_TITULAR, Audiencia.ESTUDIANTE_GRUPO, Audiencia.AMBITO_GRUPO);
    private static final Set<Audiencia> ESTUDIANTES_Y_AMBITO = EnumSet.of(Audiencia.ESTUDIANTE_GRUPO, Audiencia.AMBITO_GRUPO);
    private static final Set<Audiencia> AMBITO_Y_TITULAR = EnumSet.of(Audiencia.AMBITO_GRUPO, Audiencia.DOCENTE_TITULAR);
    private static final Set<Audiencia> ESTUDIANTES_AMBITO_Y_TITULAR =
            EnumSet.of(Audiencia.ESTUDIANTE_GRUPO, Audiencia.AMBITO_GRUPO, Audiencia.DOCENTE_TITULAR);
    private static final Set<Audiencia> SOLO_ADMINISTRADOR = EnumSet.of(Audiencia.ADMINISTRADOR);
    private static final Set<Audiencia> TITULAR_Y_DESTINATARIO = EnumSet.of(Audiencia.DOCENTE_TITULAR, Audiencia.DESTINATARIO);

    private final Map<String, Set<Audiencia>> audienciasPorTipo;

    public RealtimeAudienciaPolicy(final Map<String, Set<Audiencia>> audienciasPorTipo) {
        this.audienciasPorTipo = Map.copyOf(Objects.requireNonNull(audienciasPorTipo, "El mapa de audiencias es obligatorio."));
    }

    /** Politica vigente: tabla del contrato 3.6.3. */
    public static RealtimeAudienciaPolicy porDefecto() {
        final Map<String, Set<Audiencia>> tabla = new HashMap<>();
        // Conteos y avisos sin identidad: todo el grupo (titular, estudiantes activos y ambito de lectura).
        for (final String tipo : new String[]{"ASISTENCIAS_SESION_ACTUALIZADAS", "ASISTENCIAS_TOMA_ACTUALIZADAS", "SESION_CANCELADA"}) {
            tabla.put(tipo, GRUPO_COMPLETO);
        }
        // CULM-001 / DEC-064: sesion creada manualmente. Estudiantes del grupo y ambito de lectura (coordinador del programa).
        // DEC-065 (2): ademas el docente titular, que ve aprobada su solicitud al instante.
        tabla.put("SESION_MANUAL_CREADA", ESTUDIANTES_AMBITO_Y_TITULAR);
        // DEC-065 (2): solicitud de sesion extraordinaria pendiente. Sin estudiantes: ambito de lectura (coordinador) y titular.
        tabla.put("SESION_SOLICITUD_CREADA", AMBITO_Y_TITULAR);
        // Alertas y revisiones: el titular y los destinatarios que calcula la DB.
        for (final String tipo : new String[]{"ALERTA_INASISTENCIA", "REVISION_CANCELACION_CREADA", "REVISION_CANCELACION_DECIDIDA",
                "REVISION_CANCELACION_RESUELTA", "REVISION_CANCELACION_REASIGNADA"}) {
            tabla.put(tipo, TITULAR_Y_DESTINATARIO);
        }
        // Operacion del docente (algunos llevan token o identidad): solo el titular.
        for (final String tipo : new String[]{"ASISTENCIA_REGISTRADA", "MATRICULA_QR_ROTADO", "ESTUDIANTE_MATRICULADO", "TOMA_ABIERTA", "TOMA_CERRADA",
                "ASISTENCIA_QR_ROTADO", "ASISTENCIA_QR_REGISTRADA"}) {
            tabla.put(tipo, SOLO_TITULAR);
        }
        // Configuracion (CULM-001 D.3, HU168): el cambio de un parametro solo concierne al ADMINISTRADOR.
        tabla.put("PARAMETER_UPDATED", SOLO_ADMINISTRADOR);
        return new RealtimeAudienciaPolicy(tabla);
    }

    /** Catalogo de tipos de evento con audiencia declarada (CULM-001 D.9-c: valida el cuerpo de {@code POST /realtime/emit}). */
    public Set<String> tiposConocidos() {
        return audienciasPorTipo.keySet();
    }

    /** Audiencias del tipo por igualdad exacta; un tipo no listado (o nulo) llega solo al titular. */
    public Set<Audiencia> audienciasDe(final String tipo) {
        if (tipo == null) {
            return SOLO_TITULAR;
        }
        final Set<Audiencia> audiencias = audienciasPorTipo.get(tipo);
        return audiencias == null || audiencias.isEmpty() ? SOLO_TITULAR : audiencias;
    }

    public boolean entregaA(final String tipo, final Audiencia audiencia) {
        return audienciasDe(tipo).contains(audiencia);
    }

    /** Decision de entrega para un suscriptor con sus audiencias sobre el grupo. */
    public boolean entrega(final String tipo, final Set<Audiencia> delSuscriptor, final UUID usuarioId, final Set<UUID> destinatarios) {
        final Set<Audiencia> delTipo = audienciasDe(tipo);
        for (final Audiencia audiencia : delSuscriptor) {
            if (audiencia != Audiencia.DESTINATARIO && delTipo.contains(audiencia)) {
                return true;
            }
        }
        return delTipo.contains(Audiencia.DESTINATARIO) && destinatarios != null && usuarioId != null && destinatarios.contains(usuarioId);
    }
}
