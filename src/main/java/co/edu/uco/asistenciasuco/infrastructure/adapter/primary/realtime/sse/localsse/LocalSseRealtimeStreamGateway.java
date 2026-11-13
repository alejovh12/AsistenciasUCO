package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.localsse;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia.AudienciaRealtimeGrupo;
import co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia.primaryports.ResolverAudienciaRealtimeInputPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.RealtimeAudienciaPolicy;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.RealtimeAudienciaPolicy.Audiencia;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.contract.RealtimeStreamGateway;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.response.RealtimeEventResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.localsse.ReactorRealtimeAdapter;
import reactor.core.publisher.Flux;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

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

    private final ReactorRealtimeAdapter realtimeAdapter;
    private final ResolverAudienciaRealtimeInputPort resolverAudiencia;
    private final RealtimeAudienciaPolicy audienciaPolicy;

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
        this.realtimeAdapter = Objects.requireNonNull(realtimeAdapter, "ReactorRealtimeAdapter es requerido.");
        this.resolverAudiencia = Objects.requireNonNull(resolverAudiencia, "ResolverAudienciaRealtimeInputPort es requerido.");
        this.audienciaPolicy = Objects.requireNonNull(audienciaPolicy, "RealtimeAudienciaPolicy es requerida.");
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
    public int activeSubscribersCount() {
        return realtimeAdapter.activeSubscribersCount();
    }

    @Override
    public void emit(final String type, final Map<String, Object> payload) {
        realtimeAdapter.publish(RealtimeEvent.of(type, payload));
    }
}
