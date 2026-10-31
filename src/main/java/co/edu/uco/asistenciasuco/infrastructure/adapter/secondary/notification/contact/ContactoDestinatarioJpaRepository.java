package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contact;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResultMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResultValidator;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.StoredProcedureQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repositorio JPA para consultar en lote el contacto de destinatarios via usp_consultar_contacto_notificacion.
 */
public class ContactoDestinatarioJpaRepository implements ContactoDestinatarioResolver {

    private static final String SP_NAME = "dbo.usp_consultar_contacto_notificacion";
    private static final String OPERATION = "consultar_contacto_notificacion";

    private final EntityManager entityManager;

    public ContactoDestinatarioJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "EntityManager es obligatorio.");
    }

    @Override
    public List<ContactoDestinatario> consultarContactos(final Collection<UUID> idsUsuarios) {
        if (idsUsuarios == null || idsUsuarios.isEmpty()) {
            return List.of();
        }
        final Set<UUID> unicos = new LinkedHashSet<>();
        for (final UUID id : idsUsuarios) {
            if (id != null) {
                unicos.add(id);
            }
        }
        if (unicos.isEmpty()) {
            return List.of();
        }

        final String usuariosJson = unicos.stream()
                .map(id -> "\"" + id + "\"")
                .collect(Collectors.joining(",", "[", "]"));
        final UUID corrId = resolveCorrelationId();

        final EntityManager emDedicado = abrirEntityManagerDedicado();
        final EntityManager em = emDedicado != null ? emDedicado : entityManager;
        try {
            final StoredProcedureQuery sp = em.createStoredProcedureQuery(SP_NAME);
            sp.registerStoredProcedureParameter("usuariosJson", String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("idCorrelacion", UUID.class, ParameterMode.IN);

            sp.setParameter("usuariosJson", usuariosJson);
            sp.setParameter("idCorrelacion", corrId);

            sp.execute();

            final List<?> canonicalRows = sp.getResultList();
            final ProcedureResult canonical = ProcedureResultMapper.mapSingle(canonicalRows);
            ProcedureResultValidator.validate(canonical, canonical.getIdCorrelacion(), OPERATION);

            final List<?> contactoRows = sp.hasMoreResults() ? sp.getResultList() : List.of();
            sp.hasMoreResults();

            final List<ContactoDestinatario> resultado = new ArrayList<>(contactoRows.size());
            for (final Object cObj : contactoRows) {
                final Object[] row = (Object[]) cObj;
                final UUID idUsuario = toUuid(row[0]);
                final String correo = String.valueOf(row[1]);
                final boolean confirmado = toBoolean(row[2]);
                resultado.add(new ContactoDestinatario(idUsuario, correo, confirmado));
            }
            return resultado;
        } catch (final DatabaseOperationException e) {
            throw e;
        } catch (final ApplicationException | PersistenceException | IllegalStateException | IllegalArgumentException e) {
            throw new DatabaseOperationException(
                    DatabaseErrorCode.DATABASE_OPERATION_ERROR,
                    "No fue posible consultar los contactos de notificacion.",
                    e
            );
        } finally {
            if (emDedicado != null && emDedicado.isOpen()) {
                emDedicado.close();
            }
        }
    }

    private EntityManager abrirEntityManagerDedicado() {
        final jakarta.persistence.EntityManagerFactory emf = entityManager.getEntityManagerFactory();
        return emf != null ? emf.createEntityManager() : null;
    }

    private static UUID resolveCorrelationId() {
        final UUID corrId = CorrelationIdContext.get();
        return corrId != null ? corrId : UUID.randomUUID();
    }

    private static UUID toUuid(final Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(String.valueOf(value));
    }

    private static boolean toBoolean(final Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
