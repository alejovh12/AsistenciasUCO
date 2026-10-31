package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.VerificacionCorreoRepositoryPort;
import co.edu.uco.asistenciasuco.crosscutting.util.TokenHashHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * IDENT-001: verificacion de correo por JPA. El token llega como valor opaco y aqui se calcula su SHA-256; la base de
 * datos decide la vigencia y el uso unico. El hash y el token nunca se registran en logs.
 */
@Repository
public class VerificacionCorreoJpaRepository implements VerificacionCorreoRepositoryPort {

    static final String SQL_CONSULTAR_TOKEN = """
            EXEC dbo.usp_consultar_token_verificacion_correo
                 @tokenHash = :tokenHash,
                 @idCorrelacion = :idCorrelacion
            """;
    static final String OP_CONSULTAR_TOKEN = "consultarTokenVerificacionCorreo";

    static final String SQL_CONFIRMAR = """
            EXEC dbo.usp_confirmar_verificacion_correo
                 @tokenHash = :tokenHash,
                 @idCorrelacion = :idCorrelacion
            """;
    static final String OP_CONFIRMAR = "confirmarVerificacionCorreo";

    static final String SQL_REENVIO = """
            EXEC dbo.usp_consultar_reenvio_verificacion_correo
                 @idGrupo = :idGrupo,
                 @idEstudiante = :idEstudiante,
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion
            """;
    static final String OP_REENVIO = "consultarReenvioVerificacionCorreo";

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public VerificacionCorreoJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de verificacion de correo es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El ejecutor de procedimientos es obligatorio.");
    }

    @Override
    public Optional<UUID> consultarUsuarioPorToken(final String token) {
        final byte[] hash = TokenHashHelper.sha256(token);
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                OP_CONSULTAR_TOKEN,
                "No fue posible consultar el token de verificacion de correo desde base de datos.",
                () -> {
                    final List<?> filas = entityManager.createNativeQuery(SQL_CONSULTAR_TOKEN)
                            .setParameter("tokenHash", hash)
                            .setParameter("idCorrelacion", correlationId)
                            .getResultList();
                    return filas.isEmpty() ? Optional.<UUID>empty() : Optional.of(aUuid(filas.getFirst()));
                }
        );
    }

    @Override
    public void confirmar(final String token) {
        final byte[] hash = TokenHashHelper.sha256(token);
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("tokenHash", hash);
        parametros.put("idCorrelacion", correlationId);
        procedureExecutor.execute(OP_CONFIRMAR, SQL_CONFIRMAR, parametros, correlationId);
    }

    @Override
    public UUID consultarReenvio(final UUID grupoId, final UUID estudianteId, final UUID actorUsuarioId) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                OP_REENVIO,
                "No fue posible validar el reenvio de verificacion de correo desde base de datos.",
                () -> {
                    final List<?> filas;
                    try {
                        filas = entityManager.createNativeQuery(SQL_REENVIO)
                                .setParameter("idGrupo", grupoId)
                                .setParameter("idEstudiante", estudianteId)
                                .setParameter("idUsuarioEjecutor", actorUsuarioId)
                                .setParameter("idCorrelacion", correlationId)
                                .getResultList();
                    } catch (final PersistenceException exception) {
                        throw DbCodeTranslator.traducir(exception);
                    }
                    if (filas.isEmpty()) {
                        throw new PersistenceException("El SP de reenvio no devolvio el usuario del estudiante.");
                    }
                    return aUuid(filas.getFirst());
                }
        );
    }

    private static UUID aUuid(final Object valor) {
        final Object celda = valor instanceof Object[] fila ? fila[0] : valor;
        return celda instanceof UUID uuid ? uuid : UUID.fromString(String.valueOf(celda));
    }
}
