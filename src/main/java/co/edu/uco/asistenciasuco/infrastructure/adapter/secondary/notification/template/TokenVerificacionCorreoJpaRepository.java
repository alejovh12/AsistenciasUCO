package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResultMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResultValidator;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Registra el hash del token de verificacion con {@code usp_emitir_token_verificacion_correo}. Usa un
 * {@code EntityManager} dedicado, como el resto de repositorios del despachador, porque el hilo de fondo no tiene
 * transaccion ni sesion de Hibernate abierta. Un rechazo del SP (correo ya verificado, usuario inactivo) se traduce a
 * la excepcion semantica correspondiente.
 */
public class TokenVerificacionCorreoJpaRepository implements TokenVerificacionCorreoRepository {

    private static final String SP_NAME = "dbo.usp_emitir_token_verificacion_correo";
    private static final String OPERATION = "emitir_token_verificacion_correo";

    private final EntityManager entityManager;

    public TokenVerificacionCorreoJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "EntityManager es obligatorio.");
    }

    @Override
    public void registrar(final UUID idUsuario, final byte[] tokenHash) {
        Objects.requireNonNull(idUsuario, "idUsuario es obligatorio.");
        Objects.requireNonNull(tokenHash, "tokenHash es obligatorio.");
        final UUID correlacion = CorrelationIdContext.get() != null ? CorrelationIdContext.get() : UUID.randomUUID();

        final EntityManagerFactory fabrica = entityManager.getEntityManagerFactory();
        final EntityManager dedicado = fabrica != null ? fabrica.createEntityManager() : null;
        final EntityManager em = dedicado != null ? dedicado : entityManager;
        try {
            final StoredProcedureQuery sp = em.createStoredProcedureQuery(SP_NAME);
            sp.registerStoredProcedureParameter("idUsuario", UUID.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("tokenHash", byte[].class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("idCorrelacion", UUID.class, ParameterMode.IN);
            sp.setParameter("idUsuario", idUsuario);
            sp.setParameter("tokenHash", tokenHash);
            sp.setParameter("idCorrelacion", correlacion);
            sp.execute();

            final List<?> filas = sp.getResultList();
            final ProcedureResult canonico = ProcedureResultMapper.mapSingle(filas);
            ProcedureResultValidator.validate(canonico, correlacion, OPERATION);
        } finally {
            if (dedicado != null && dedicado.isOpen()) {
                dedicado.close();
            }
        }
    }
}
