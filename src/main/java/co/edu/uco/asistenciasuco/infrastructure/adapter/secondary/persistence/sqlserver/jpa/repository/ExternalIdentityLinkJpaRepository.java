package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.secondaryports.security.ExternalIdentityConflictException;
import co.edu.uco.asistenciasuco.application.secondaryports.security.ExternalIdentityLinkPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Vínculo persistente {@code issuer + subject → Usuario.id} (SEC-BASELINE-017). La lectura usa la vista
 * {@code uv_identidad_externa}; el alta y la revocación pasan por los SP públicos canónicos
 * {@code usp_vincular_identidad_externa} y {@code usp_revocar_identidad_externa}, que serializan y rechazan
 * conflictos de forma atómica en la DB.
 */
@Repository
public class ExternalIdentityLinkJpaRepository implements ExternalIdentityLinkPort {

    static final String HQL_VINCULO = """
            select i.idUsuario, i.estaVigenteIdentidad
            from UvIdentidadExternaEntity i
            where i.emisor = :emisor and i.sujeto = :sujeto
            """;
    static final String HQL_SUJETOS_VIGENTES = """
            select i.sujeto
            from UvIdentidadExternaEntity i
            where i.idUsuario = :idUsuario and i.estaVigenteIdentidad = true
            order by i.emisor
            """;
    static final String SQL_VINCULAR = """
            EXEC dbo.usp_vincular_identidad_externa
                 @emisor = :emisor,
                 @sujeto = :sujeto,
                 @idUsuario = :idUsuario,
                 @correoVinculacion = :correoVinculacion,
                 @idCorrelacion = :idCorrelacion
            """;
    static final String SQL_REVOCAR = """
            EXEC dbo.usp_revocar_identidad_externa
                 @emisor = :emisor,
                 @sujeto = :sujeto,
                 @idCorrelacion = :idCorrelacion
            """;

    private static final String PARAM_EMISOR = "emisor";
    private static final String PARAM_SUJETO = "sujeto";

    static final String OP_BUSCAR = "buscarIdentidadExterna";
    static final String OP_BUSCAR_POR_USUARIO = "buscarIdentidadesExternasPorUsuario";
    static final String OP_VINCULAR = "vincularIdentidadExterna";
    static final String OP_REVOCAR = "revocarIdentidadExterna";

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public ExternalIdentityLinkJpaRepository(
            final EntityManager entityManager,
            final JpaProcedureExecutor procedureExecutor
    ) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El JpaProcedureExecutor es obligatorio.");
    }

    @Override
    public Optional<ExternalIdentityLink> find(final String issuer, final String subject) {
        return JpaQueryExecutor.execute(
                OP_BUSCAR,
                "No fue posible consultar el vínculo de identidad externa.",
                () -> {
                    final List<Object[]> rows = entityManager.createQuery(HQL_VINCULO, Object[].class)
                            .setParameter(PARAM_EMISOR, issuer)
                            .setParameter(PARAM_SUJETO, subject)
                            .setMaxResults(2)
                            .getResultList();
                    if (rows.isEmpty()) {
                        return Optional.<ExternalIdentityLink>empty();
                    }
                    if (rows.size() > 1) {
                        // UNIQUE(emisor, sujeto) lo impide; si ocurriera, no se elige una fila.
                        throw new IllegalStateException("Vínculo de identidad externa duplicado.");
                    }
                    final Object[] row = rows.getFirst();
                    return Optional.of(new ExternalIdentityLink((UUID) row[0], Boolean.TRUE.equals(row[1])));
                });
    }

    @Override
    public List<String> findSujetosVigentes(final UUID usuarioId) {
        return JpaQueryExecutor.execute(
                OP_BUSCAR_POR_USUARIO,
                "No fue posible consultar los vínculos de identidad externa del usuario.",
                () -> entityManager.createQuery(HQL_SUJETOS_VIGENTES, String.class)
                        .setParameter("idUsuario", usuarioId)
                        .getResultList());
    }

    @Override
    public boolean link(final String issuer, final String subject, final UUID usuarioId, final String verifiedEmail) {
        final boolean alreadyLinked = find(issuer, subject)
                .filter(link -> link.vigente() && usuarioId.equals(link.usuarioId()))
                .isPresent();
        final UUID correlationId = correlationId();
        final Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put(PARAM_EMISOR, issuer);
        parameters.put(PARAM_SUJETO, subject);
        parameters.put("idUsuario", usuarioId);
        parameters.put("correoVinculacion", verifiedEmail);
        parameters.put("idCorrelacion", correlationId);
        try {
            procedureExecutor.execute(OP_VINCULAR, SQL_VINCULAR, parameters, correlationId);
        } catch (ConflictException _) {
            throw new ExternalIdentityConflictException("La identidad externa entra en conflicto con otro vínculo.");
        }
        return !alreadyLinked;
    }

    @Override
    public void revoke(final String issuer, final String subject) {
        final UUID correlationId = correlationId();
        final Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put(PARAM_EMISOR, issuer);
        parameters.put(PARAM_SUJETO, subject);
        parameters.put("idCorrelacion", correlationId);
        procedureExecutor.execute(OP_REVOCAR, SQL_REVOCAR, parameters, correlationId);
    }

    /** La autenticación JWT puede ejecutarse antes de que exista el contexto de correlación del request. */
    private static UUID correlationId() {
        final UUID current = CorrelationIdContext.get();
        return current != null ? current : UUID.randomUUID();
    }
}
