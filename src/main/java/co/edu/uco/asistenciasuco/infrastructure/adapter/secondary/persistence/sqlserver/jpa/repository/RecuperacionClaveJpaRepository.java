package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.RecuperacionClaveRepositoryPort;
import co.edu.uco.asistenciasuco.crosscutting.util.TokenHashHelper;
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
 * HU177: enlace de recuperacion de contrasena por JPA. El token llega como valor opaco y aqui se calcula su SHA-256; la
 * base de datos decide la vigencia, la espera entre enlaces y el uso unico. El hash, el token y el correo consultado
 * nunca se registran en logs.
 */
@Repository
public class RecuperacionClaveJpaRepository implements RecuperacionClaveRepositoryPort {

    private static final String PARAM_ID_CORRELACION = "idCorrelacion";
    private static final String PARAM_TOKEN_HASH = "tokenHash";

    static final String SQL_SOLICITANTE = """
            EXEC dbo.usp_consultar_solicitud_recuperacion_clave
                 @correo = :correo,
                 @idCorrelacion = :idCorrelacion
            """;
    static final String OP_SOLICITANTE = "consultarSolicitudRecuperacionClave";

    static final String SQL_DUENO = """
            EXEC dbo.usp_consultar_token_recuperacion_clave
                 @tokenHash = :tokenHash,
                 @idCorrelacion = :idCorrelacion
            """;
    static final String OP_DUENO = "consultarTokenRecuperacionClave";

    static final String SQL_CONSUMIR = """
            EXEC dbo.usp_consumir_token_recuperacion_clave
                 @tokenHash = :tokenHash,
                 @idCorrelacion = :idCorrelacion
            """;
    static final String OP_CONSUMIR = "consumirTokenRecuperacionClave";

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public RecuperacionClaveJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de recuperacion de contrasena es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El ejecutor de procedimientos es obligatorio.");
    }

    @Override
    public Optional<Solicitante> consultarSolicitante(final String correo) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                OP_SOLICITANTE,
                "No fue posible consultar la solicitud de recuperacion de contrasena desde base de datos.",
                () -> primeraFila(entityManager.createNativeQuery(SQL_SOLICITANTE)
                        .setParameter("correo", correo)
                        .setParameter(PARAM_ID_CORRELACION, correlationId)
                        .getResultList())
                        .map(fila -> new Solicitante(aUuid(fila[0]), fila.length > 1 && fila[1] != null ? String.valueOf(fila[1]) : null))
        );
    }

    @Override
    public Optional<UUID> consultarUsuarioPorToken(final String token) {
        final byte[] hash = TokenHashHelper.sha256(token);
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                OP_DUENO,
                "No fue posible consultar el enlace de recuperacion de contrasena desde base de datos.",
                () -> primeraFila(entityManager.createNativeQuery(SQL_DUENO)
                        .setParameter(PARAM_TOKEN_HASH, hash)
                        .setParameter(PARAM_ID_CORRELACION, correlationId)
                        .getResultList())
                        .map(fila -> aUuid(fila[0]))
        );
    }

    @Override
    public void consumir(final String token) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put(PARAM_TOKEN_HASH, TokenHashHelper.sha256(token));
        parametros.put(PARAM_ID_CORRELACION, correlationId);
        procedureExecutor.execute(OP_CONSUMIR, SQL_CONSUMIR, parametros, correlationId);
    }

    private static Optional<Object[]> primeraFila(final List<?> filas) {
        if (filas.isEmpty()) {
            return Optional.empty();
        }
        final Object primera = filas.getFirst();
        return Optional.of(primera instanceof Object[] columnas ? columnas : new Object[] {primera});
    }

    private static UUID aUuid(final Object celda) {
        return celda instanceof UUID uuid ? uuid : UUID.fromString(String.valueOf(celda));
    }
}
