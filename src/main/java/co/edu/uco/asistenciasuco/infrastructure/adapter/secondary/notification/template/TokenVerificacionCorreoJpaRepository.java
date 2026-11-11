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
    /** HU177: SP que registra el hash del enlace para definir una contrasena nueva (misma firma y resultset). */
    public static final String SP_RECUPERACION_CLAVE = "dbo.usp_emitir_token_recuperacion_clave";
    public static final String OPERACION_RECUPERACION_CLAVE = "emitir_token_recuperacion_clave";

    private final EntityManager entityManager;
    private final String procedimiento;
    private final String operacion;

    public TokenVerificacionCorreoJpaRepository(final EntityManager entityManager) {
        this(entityManager, SP_NAME, OPERATION);
    }

    /** Para otro token de un solo uso cuyo SP de emision tiene la misma firma (idUsuario, tokenHash, idCorrelacion). */
    public TokenVerificacionCorreoJpaRepository(
            final EntityManager entityManager, final String procedimiento, final String operacion
    ) {
        this.entityManager = Objects.requireNonNull(entityManager, "EntityManager es obligatorio.");
        this.procedimiento = Objects.requireNonNull(procedimiento, "El procedimiento de emision es obligatorio.");
        this.operacion = Objects.requireNonNull(operacion, "La operacion es obligatoria.");
    }

    @Override
    public void registrar(final UUID idUsuario, final byte[] tokenHash) {
        Objects.requireNonNull(idUsuario, "idUsuario es obligatorio.");
        Objects.requireNonNull(tokenHash, "tokenHash es obligatorio.");
        final UUID correlacion = CorrelationIdContext.get() != null ? CorrelationIdContext.get() : UUID.randomUUID();

        final EntityManager dedicado = abrirEntityManagerDedicado();
        final EntityManager em = dedicado != null ? dedicado : entityManager;
        try {
            final StoredProcedureQuery sp = em.createStoredProcedureQuery(procedimiento);
            sp.registerStoredProcedureParameter("idUsuario", UUID.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("tokenHash", byte[].class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("idCorrelacion", UUID.class, ParameterMode.IN);
            sp.setParameter("idUsuario", idUsuario);
            sp.setParameter("tokenHash", tokenHash);
            sp.setParameter("idCorrelacion", correlacion);
            sp.execute();

            final List<?> filas = sp.getResultList();
            final ProcedureResult canonico = ProcedureResultMapper.mapSingle(filas);
            ProcedureResultValidator.validate(canonico, correlacion, operacion);
        } finally {
            if (dedicado != null && dedicado.isOpen()) {
                dedicado.close();
            }
        }
    }

    private EntityManager abrirEntityManagerDedicado() {
        final EntityManagerFactory fabrica = entityManager.getEntityManagerFactory();
        return fabrica != null ? fabrica.createEntityManager() : null;
    }
}
