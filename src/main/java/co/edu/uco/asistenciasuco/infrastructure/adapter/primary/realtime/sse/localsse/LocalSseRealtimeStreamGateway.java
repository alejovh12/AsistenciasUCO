package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.localsse;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia.AudienciaRealtimeGrupo;
import co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia.primaryports.ResolverAudienciaRealtimeInputPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.RealtimeAudienciaPolicy;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.RealtimeAudienciaPolicy.Audiencia;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.contract.RealtimeStreamGateway;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.response.RealtimeEventResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.localsse.ReactorRealtimeAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Bridge interno entre el controller SSE y el adapter local seleccionado por el Composition Root.
 *
 * <p>No se autoregistra con Spring. {@code LocalSseRealtimeAdapterConfiguration} crea esta
 * implementacion solamente cuando el provider realtime es {@code local-sse}.</p>
 *
 * <p>Autorizacion y scope por grupo (CULM-001 C.1, F-TRX-006, DEC-022 §1): las audiencias del usuario sobre {@code grupoId}
 * (docente titular, estudiante activo, ambito de lectura de coordinador, decano o administrador) se resuelven UNA vez, en la DB, al
 * establecer la suscripcion (instantanea) y antes de construir el {@link Flux}; sin ninguna audiencia responde 403. Despues, el stream
 * se filtra EN MEMORIA: solo eventos cuyo {@code payload.grupo} coincide con el grupo solicitado y que
 * {@link RealtimeAudienciaPolicy#entrega} autoriza para las audiencias del suscriptor y los destinatarios del evento.</p>
 */
public final class LocalSseRealtimeStreamGateway implements RealtimeStreamGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalSseRealtimeStreamGateway.class);

    /**
     * REDIS-001 (TD-084): tipos que el canal del usuario entrega por ambito sobre el grupo del evento (contador de
     * solicitudes de sesion del menu). Solo a titular o ambito: los estudiantes los reciben en el canal del grupo.
     */
    static final Set<String> TIPOS_CANAL_USUARIO_POR_GRUPO =
            Set.of("SESION_SOLICITUD_CREADA", "SESION_SOLICITUD_RECHAZADA", "SESION_MANUAL_CREADA");
    private static final Set<Audiencia> AUDIENCIAS_CANAL_USUARIO = EnumSet.of(Audiencia.DOCENTE_TITULAR, Audiencia.AMBITO_GRUPO);
    private static final String ROL_ESTUDIANTE = "ROLE_ESTUDIANTE";
    /**
     * Code review 2026-11-26: el ambito resuelto para un grupo en el canal del usuario se recuerda solo este tiempo; asi un
     * usuario que pierde el ambito deja de recibir los eventos de ese grupo sin tener que reconectarse.
     */
    static final Duration VIGENCIA_AMBITO = Duration.ofSeconds(60);
    /** Maximo de grupos recordados por conexion (al superarlo se olvidan todos y se vuelven a resolver). */
    static final int MAXIMO_AMBITOS_POR_CONEXION = 256;

    private record AmbitoRecordado(Set<Audiencia> audiencias, long venceNanos) {
    }

    private final ReactorRealtimeAdapter realtimeAdapter;
    private final ResolverAudienciaRealtimeInputPort resolverAudiencia;
    private final RealtimeAudienciaPolicy audienciaPolicy;
    private final RealtimePublisherPort publicador;
    private LongSupplier relojNanos = System::nanoTime;

    public LocalSseRealtimeStreamGateway(
            final ReactorRealtimeAdapter realtimeAdapter,
            final ResolverAudienciaRealtimeInputPort resolverAudiencia
    ) {
        this(realtimeAdapter, resolverAudiencia, RealtimeAudienciaPolicy.porDefecto());
    }

    public LocalSseRealtimeStreamGateway(
            final ReactorRealtimeAdapter realtimeAdapter,
            final ResolverAudienciaRealtimeInputPort resolverAudiencia,
            final RealtimeAudienciaPolicy audienciaPolicy
    ) {
        this(realtimeAdapter, resolverAudiencia, audienciaPolicy, (RealtimePublisherPort) realtimeAdapter);
    }

    private LocalSseRealtimeStreamGateway(
            final ReactorRealtimeAdapter realtimeAdapter,
            final ResolverAudienciaRealtimeInputPort resolverAudiencia,
            final RealtimeAudienciaPolicy audienciaPolicy,
            final RealtimePublisherPort publicador
    ) {
        this.realtimeAdapter = Objects.requireNonNull(realtimeAdapter, "ReactorRealtimeAdapter es requerido.");
        this.resolverAudiencia = Objects.requireNonNull(resolverAudiencia, "ResolverAudienciaRealtimeInputPort es requerido.");
        this.audienciaPolicy = Objects.requireNonNull(audienciaPolicy, "RealtimeAudienciaPolicy es requerida.");
        this.publicador = Objects.requireNonNull(publicador, "RealtimePublisherPort es requerido.");
    }

    /**
     * REDIS-001: con el relevo Redis, {@link #emit} publica por el relevo para que el evento llegue a todas las replicas;
     * la suscripcion sigue leyendo del sink local de esta replica.
     */
    public static LocalSseRealtimeStreamGateway conRelevo(
            final ReactorRealtimeAdapter realtimeAdapter,
            final ResolverAudienciaRealtimeInputPort resolverAudiencia,
            final RealtimePublisherPort publicador
    ) {
        return new LocalSseRealtimeStreamGateway(realtimeAdapter, resolverAudiencia, RealtimeAudienciaPolicy.porDefecto(), publicador);
    }

    @Override
    public Flux<RealtimeEventResponse> subscribe(final UUID usuarioId, final UUID grupoId) {
        Objects.requireNonNull(usuarioId, "El usuario autenticado es obligatorio para suscribirse a realtime.");
        Objects.requireNonNull(grupoId, "El grupoId es obligatorio para suscribirse a realtime.");
        final AudienciaRealtimeGrupo audiencia = resolverAudiencia.execute(new ResolverAudienciaRealtimeInputPort.Comando(usuarioId, grupoId));
        if (audiencia == null || !audiencia.tieneAlguna()) {
            throw new ForbiddenException(MensajeUsuarioErrorCode.ACC_APP_017);
        }
        final Set<Audiencia> delSuscriptor = EnumSet.noneOf(Audiencia.class);
        if (audiencia.docenteTitular()) {
            delSuscriptor.add(Audiencia.DOCENTE_TITULAR);
        }
        if (audiencia.estudianteGrupo()) {
            delSuscriptor.add(Audiencia.ESTUDIANTE_GRUPO);
        }
        if (audiencia.ambitoGrupo()) {
            delSuscriptor.add(Audiencia.AMBITO_GRUPO);
        }

        final String grupoIdTexto = grupoId.toString();
        return realtimeAdapter.events()
                .filter(event -> grupoIdTexto.equals(event.payload().get("grupo")))
                .filter(event -> audienciaPolicy.entrega(event.type(), delSuscriptor, usuarioId, event.destinatariosUsuario()))
                .map(RealtimeEventResponse::from);
    }

    @Override
    public Flux<RealtimeEventResponse> subscribeUsuario(final UUID usuarioId) {
        return subscribeUsuario(usuarioId, !soloEstudiante(SecurityContextHolder.getContext().getAuthentication()));
    }

    /**
     * {@code porGrupo=false} (usuario que solo es estudiante) evita consultar ambitos: solo recibe lo que le llega como
     * destinatario. La consulta de ambito corre fuera del hilo que publica el evento y se recuerda por grupo durante
     * {@link #VIGENCIA_AMBITO}; si falla, ese evento no se entrega y se reintenta con el siguiente.
     */
    Flux<RealtimeEventResponse> subscribeUsuario(final UUID usuarioId, final boolean porGrupo) {
        Objects.requireNonNull(usuarioId, "El usuario autenticado es obligatorio para suscribirse a realtime.");
        final Map<UUID, AmbitoRecordado> ambitos = new ConcurrentHashMap<>();
        return realtimeAdapter.events()
                .filter(event -> esDestinatario(event, usuarioId) || (porGrupo && grupoParaCanalUsuario(event) != null))
                .publishOn(Schedulers.boundedElastic())
                .filter(event -> esDestinatario(event, usuarioId) || entregaPorGrupo(event, usuarioId, ambitos))
                .map(RealtimeEventResponse::from);
    }

    private boolean esDestinatario(final RealtimeEvent event, final UUID usuarioId) {
        return event.destinatariosUsuario().contains(usuarioId) && audienciaPolicy.entregaA(event.type(), Audiencia.DESTINATARIO);
    }

    private static UUID grupoParaCanalUsuario(final RealtimeEvent event) {
        if (!TIPOS_CANAL_USUARIO_POR_GRUPO.contains(event.type())) {
            return null;
        }
        final Object grupo = event.payload().get("grupo");
        if (grupo == null) {
            return null;
        }
        try {
            return UUID.fromString(grupo.toString());
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean entregaPorGrupo(final RealtimeEvent event, final UUID usuarioId, final Map<UUID, AmbitoRecordado> ambitos) {
        final UUID grupo = grupoParaCanalUsuario(event);
        if (grupo == null) {
            return false;
        }
        final long ahora = relojNanos.getAsLong();
        final AmbitoRecordado recordado = ambitos.get(grupo);
        final Set<Audiencia> delSuscriptor;
        if (recordado != null && ahora - recordado.venceNanos() < 0) {
            delSuscriptor = recordado.audiencias();
        } else {
            try {
                delSuscriptor = audienciasCanalUsuario(resolverAudiencia.execute(new ResolverAudienciaRealtimeInputPort.Comando(usuarioId, grupo)));
            } catch (final RuntimeException exception) {
                LOGGER.warn("No se pudo resolver el ambito realtime del usuario sobre el grupo; evento {} no entregado. causa={}",
                        event.type(), exception.getClass().getSimpleName());
                return false;
            }
            if (ambitos.size() >= MAXIMO_AMBITOS_POR_CONEXION) {
                ambitos.clear();
            }
            ambitos.put(grupo, new AmbitoRecordado(delSuscriptor, ahora + VIGENCIA_AMBITO.toNanos()));
        }
        return !delSuscriptor.isEmpty() && audienciaPolicy.entrega(event.type(), delSuscriptor, usuarioId, event.destinatariosUsuario());
    }

    private static Set<Audiencia> audienciasCanalUsuario(final AudienciaRealtimeGrupo audiencia) {
        final Set<Audiencia> resultado = EnumSet.noneOf(Audiencia.class);
        if (audiencia != null && audiencia.docenteTitular()) {
            resultado.add(Audiencia.DOCENTE_TITULAR);
        }
        if (audiencia != null && audiencia.ambitoGrupo()) {
            resultado.add(Audiencia.AMBITO_GRUPO);
        }
        resultado.retainAll(AUDIENCIAS_CANAL_USUARIO);
        return resultado;
    }

    /** true solo si la autenticacion trae roles y todos son ESTUDIANTE; sin roles conocidos se resuelve por ambito (correcto, mas consultas). */
    static boolean soloEstudiante(final Authentication autenticacion) {
        if (autenticacion == null || autenticacion.getAuthorities() == null || autenticacion.getAuthorities().isEmpty()) {
            return false;
        }
        for (final GrantedAuthority autoridad : autenticacion.getAuthorities()) {
            if (!ROL_ESTUDIANTE.equals(autoridad.getAuthority())) {
                return false;
            }
        }
        return true;
    }

    /** Solo para pruebas: reloj monotono en nanosegundos que decide el vencimiento del ambito recordado. */
    void usarRelojNanos(final LongSupplier reloj) {
        this.relojNanos = Objects.requireNonNull(reloj, "El reloj es obligatorio.");
    }

    @Override
    public int activeSubscribersCount() {
        return realtimeAdapter.activeSubscribersCount();
    }

    @Override
    public void emit(final String type, final Map<String, Object> payload) {
        publicador.publish(RealtimeEvent.of(type, payload));
    }
}
