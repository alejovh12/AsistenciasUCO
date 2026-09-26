package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.AsistenciaQueryPersistence;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.AsistenciaJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.AsistenciaQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * Query JPA (HQL, no native) de asistencias por grupo sobre las vistas congeladas
 * {@code uv_detalle_asistencia}, {@code uv_asistencia} y {@code uv_estudiante_grupo}. Reproduce la
 * semantica del SQL JDBC baseline: mismo join, mismo filtro exacto por grupo y filtro opcional por
 * sesion, sin ORDER BY, sin filas sinteticas y {@code observacion} vacia.
 *
 * <p>Sin transaccion: es una lectura de una sola sentencia con autocommit; no usa
 * {@code SESSION_CONTEXT} ni vistas {@code uv_auth_*} y no participa de la autorizacion.</p>
 */
public final class AsistenciaJpaQueryPersistence implements AsistenciaQueryPersistence {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsistenciaJpaQueryPersistence.class);

    private static final String PARAM_GRUPO = "grupo";
    private static final String PARAM_SESION = "sesion";

    static final String HQL_CONSULTAR_ASISTENCIAS = """
            select new co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.AsistenciaQueryRow(
                da.id, eg.idEstudiante, eg.idGrupo, a.idSesion, da.asistio, da.codigoRazonCausa)
            from UvDetalleAsistenciaEntity da
            join UvAsistenciaEntity a on a.id = da.idAsistencia
            join UvEstudianteGrupoEntity eg on eg.id = a.idEstudianteGrupo
            where eg.idGrupo = :grupo
              and (:sesion is null or a.idSesion = :sesion)
            """;

    private final EntityManagerFactory entityManagerFactory;

    public AsistenciaJpaQueryPersistence(final EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = Objects.requireNonNull(
                entityManagerFactory,
                "El EntityManagerFactory de la query de asistencia es obligatorio."
        );
    }

    @Override
    public List<AsistenciaRepositoryProjection> consultarAsistenciasPorGrupo(
            final ConsultarAsistenciasPorGrupoRepositoryDTO dto
    ) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para consultar asistencias por grupo es obligatorio.");
        }

        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            return entityManager.createQuery(HQL_CONSULTAR_ASISTENCIAS, AsistenciaQueryRow.class)
                    .setParameter(PARAM_GRUPO, dto.getGrupo())
                    .setParameter(PARAM_SESION, dto.getSesion())
                    .getResultList()
                    .stream()
                    .map(AsistenciaJpaProjectionMapper::toProjection)
                    .toList();
        } catch (PersistenceException | IllegalStateException | IllegalArgumentException exception) {
            LOGGER.error(
                    "SQL operation failed. operation=consultarAsistenciasPorGrupo, correlationId={}",
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException("No fue posible consultar las asistencias de base de datos.", exception);
        }
    }
}
