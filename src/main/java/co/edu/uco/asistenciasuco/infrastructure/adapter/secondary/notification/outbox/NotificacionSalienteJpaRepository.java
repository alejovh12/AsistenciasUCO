package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureRow;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador de persistencia JPA para el outbox de notificaciones salientes.
 */
@Repository
public class NotificacionSalienteJpaRepository implements NotificacionOutboxRepository {

    private final JpaProcedureExecutor procedureExecutor;
    private final EntityManager entityManager;

    public NotificacionSalienteJpaRepository(
            final JpaProcedureExecutor procedureExecutor,
            final EntityManager entityManager
    ) {
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "JpaProcedureExecutor es obligatorio.");
        this.entityManager = Objects.requireNonNull(entityManager, "EntityManager es obligatorio.");
    }

    @Override
    public void registrar(final RegistroNotificacion registro) {
        Objects.requireNonNull(registro, "El registro de notificacion es obligatorio.");
        final UUID corrId = registro.idCorrelacion() != null ? registro.idCorrelacion() : resolveCorrelationId();

        final Map<String, Object> params = new HashMap<>();
        params.put("tipo", registro.tipo());
        params.put("asunto", registro.asunto());
        params.put("mensaje", registro.mensaje());
        params.put("destinatariosJson", registro.destinatariosJson());
        params.put("datosJson", registro.datosJson());
        params.put("idCorrelacion", corrId);
        params.put("traceId", registro.traceId());
        params.put("spanId", registro.spanId());

        final String sql = "EXEC dbo.usp_registrar_notificacion_saliente "
                + "@tipo = :tipo, @asunto = :asunto, @mensaje = :mensaje, "
                + "@destinatariosJson = :destinatariosJson, @datosJson = :datosJson, "
                + "@idCorrelacion = :idCorrelacion, @traceId = :traceId, @spanId = :spanId";

        procedureExecutor.execute("registrar_notificacion_saliente", sql, params, corrId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<NotificacionReservada> reservar(final int lote, final Duration lease, final String instancia) {
        Objects.requireNonNull(lease, "El lease es obligatorio.");
        Objects.requireNonNull(instancia, "La instancia es obligatoria.");
        final UUID corrId = resolveCorrelationId();

        final String sql = "EXEC dbo.usp_reservar_notificaciones_salientes "
                + "@lote = :lote, @leaseSegundos = :leaseSegundos, @instancia = :instancia, @idCorrelacion = :idCorrelacion";

        final Query query = entityManager.createNativeQuery(sql)
                .setParameter("lote", lote)
                .setParameter("leaseSegundos", (int) lease.toSeconds())
                .setParameter("instancia", instancia)
                .setParameter("idCorrelacion", corrId);

        final List<Object> filas = query.getResultList();
        final List<NotificacionReservada> resultado = new ArrayList<>();

        for (final Object filaObj : filas) {
            final Object[] fila = (Object[]) filaObj;
            final UUID id = toUuid(fila[0]);
            final String tipo = (String) fila[1];
            final String asunto = (String) fila[2];
            final String mensaje = (String) fila[3];
            final String destinatarios = (String) fila[4];
            final String datos = (String) fila[5];
            final int intentos = toInt(fila[6]);
            final Instant creadoEn = toInstant(fila[7]);
            final UUID idCorrelacion = toUuid(fila[8]);
            final String traceId = (String) fila[9];
            final String spanId = (String) fila[10];

            resultado.add(new NotificacionReservada(
                    id,
                    tipo,
                    asunto,
                    mensaje,
                    destinatarios,
                    datos,
                    intentos,
                    creadoEn,
                    idCorrelacion,
                    traceId,
                    spanId
            ));
        }

        return resultado;
    }

    @Override
    public void marcarEntregada(final UUID id, final String instancia) {
        Objects.requireNonNull(id, "El id de notificacion es obligatorio.");
        Objects.requireNonNull(instancia, "La instancia es obligatoria.");
        final UUID corrId = resolveCorrelationId();

        final Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("instancia", instancia);
        params.put("idCorrelacion", corrId);

        final String sql = "EXEC dbo.usp_marcar_notificacion_entregada "
                + "@id = :id, @instancia = :instancia, @idCorrelacion = :idCorrelacion";

        procedureExecutor.execute("marcar_notificacion_entregada", sql, params, corrId);
    }

    @Override
    public void registrarFallo(
            final UUID id,
            final String instancia,
            final String codigoError,
            final String detalle,
            final boolean reintentable,
            final int maxIntentos,
            final Duration base,
            final Duration max
    ) {
        Objects.requireNonNull(id, "El id de notificacion es obligatorio.");
        Objects.requireNonNull(instancia, "La instancia es obligatoria.");
        final UUID corrId = resolveCorrelationId();

        final Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("instancia", instancia);
        params.put("codigoError", codigoError != null ? codigoError : "ERR_DESCONOCIDO");
        params.put("detalleError", detalle);
        params.put("reintentable", reintentable);
        params.put("maxIntentos", maxIntentos);
        params.put("backoffBaseSegundos", base != null ? (int) base.toSeconds() : 30);
        params.put("backoffMaxSegundos", max != null ? (int) max.toSeconds() : 900);
        params.put("idCorrelacion", corrId);

        final String sql = "EXEC dbo.usp_registrar_fallo_notificacion "
                + "@id = :id, @instancia = :instancia, @codigoError = :codigoError, "
                + "@detalleError = :detalleError, @reintentable = :reintentable, "
                + "@maxIntentos = :maxIntentos, @backoffBaseSegundos = :backoffBaseSegundos, "
                + "@backoffMaxSegundos = :backoffMaxSegundos, @idCorrelacion = :idCorrelacion";

        procedureExecutor.execute("registrar_fallo_notificacion", sql, params, corrId);
    }

    @Override
    public int depurar(final int retencionDias, final int lote) {
        final UUID corrId = resolveCorrelationId();

        final Map<String, Object> params = new HashMap<>();
        params.put("retencionDias", retencionDias);
        params.put("lote", lote);
        params.put("idCorrelacion", corrId);

        final String sql = "EXEC dbo.usp_depurar_notificaciones_salientes "
                + "@retencionDias = :retencionDias, @lote = :lote, @idCorrelacion = :idCorrelacion";

        final ProcedureRow row = procedureExecutor.executeWithExtras("depurar_notificaciones_salientes", sql, params, corrId, 1);
        return toInt(row.extras().get(0));
    }

    private UUID resolveCorrelationId() {
        final UUID corrId = CorrelationIdContext.get();
        return corrId != null ? corrId : UUID.randomUUID();
    }

    private static UUID toUuid(final Object value) {
        if (value == null) return null;
        if (value instanceof UUID uuid) return uuid;
        return UUID.fromString(value.toString());
    }

    private static Instant toInstant(final Object value) {
        if (value == null) return null;
        if (value instanceof Instant i) return i;
        if (value instanceof java.sql.Timestamp ts) return ts.toInstant();
        if (value instanceof java.time.OffsetDateTime odt) return odt.toInstant();
        if (value instanceof java.time.ZonedDateTime zdt) return zdt.toInstant();
        String str = value.toString().trim();
        str = str.replaceFirst(" ", "T").replace(" +", "+").replace(" -", "-");
        try {
            return java.time.OffsetDateTime.parse(str).toInstant();
        } catch (final Exception e) {
            return Instant.parse(str);
        }
    }

    private static int toInt(final Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.intValue();
        return Integer.parseInt(value.toString());
    }
}
