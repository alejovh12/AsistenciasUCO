package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeAudienciaQueryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.CancelacionRowSupport.bit;

/** CULM-001 C.1 (F-TRX-006): audiencias del SSE por ambito via {@code usp_consultar_audiencias_realtime_grupo}. */
@Repository
public class RealtimeAudienciaJpaRepository implements RealtimeAudienciaQueryPort {

    private final EntityManager entityManager;

    public RealtimeAudienciaJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de audiencias es obligatorio.");
    }

    @Override
    public Audiencias consultar(final UUID usuarioId, final UUID grupoId) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarAudienciasRealtimeGrupo", "No fue posible consultar las audiencias del grupo.", () -> {
            final Map<String, Object> parametros = new LinkedHashMap<>();
            parametros.put("idUsuarioEjecutor", usuarioId);
            parametros.put("idCorrelacion", correlationId);
            parametros.put("idGrupo", grupoId);
            final List<?> filas;
            try {
                filas = CancelacionRowSupport.ejecutar(entityManager, "usp_consultar_audiencias_realtime_grupo", parametros);
            } catch (final PersistenceException exception) {
                throw DbCodeTranslator.traducir(exception, "grupo");
            }
            if (filas.isEmpty()) {
                return new Audiencias(false, false, false);
            }
            final Object[] c = (Object[]) filas.getFirst();
            return new Audiencias(bit(c[0]), bit(c[1]), bit(c[2]));
        });
    }
}
