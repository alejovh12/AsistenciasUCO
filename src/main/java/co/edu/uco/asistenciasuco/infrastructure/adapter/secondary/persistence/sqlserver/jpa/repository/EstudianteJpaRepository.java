package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.EstudianteRepositoryPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import org.springframework.stereotype.Repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarEstudiantesRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.*;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.CoreViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvEstudianteEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.EstudianteResumenQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EstudianteJpaRepository implements EstudianteRepositoryPort {
    private static final String BASE_FROM = " from UvEstudianteIdentidadEntity e join UvUsuarioEntity u on e.idUsuario = u.id";
    private static final String SELECT_RESUMEN = """
            select new co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.EstudianteResumenQueryRow(
                e.id, u.id, u.idTipoIdentificacion, u.numeroIdentificacion, u.primerApellido,
                u.segundoApellido, u.primerNombre, u.segundoNombre, u.nombreCompleto, u.correo,
                u.estaActivoUsuario)
            """;
    private static final String ORDER = " order by u.primerApellido, u.primerNombre, u.numeroIdentificacion, e.id";
    static final String HQL_CONTEXTOS = """
            select distinct a
            from UvEstudianteEntity a
            where a.id = :estudianteId
            order by a.nombreInstitucion, a.nombreFacultad, a.nombrePrograma,
                a.nombreAsignatura, a.nombreGrupo, a.idGrupo
            """;
    private final EntityManager entityManager;

    public EstudianteJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de consultas de estudiante es obligatorio.");
    }

    @Override
    public EstudiantePaginaRepositoryProjection consultarEstudiantes(final ConsultarEstudiantesRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para consultar estudiantes es obligatorio.");
        }
        return JpaQueryExecutor.execute(
                "consultarEstudiantes",
                "No fue posible consultar los estudiantes.",
                () -> {
                    final Map<String, Object> parameters = new LinkedHashMap<>();
                    final String where = buildWhere(dto, parameters);
                    final TypedQuery<Long> countQuery =
                            entityManager.createQuery("select count(e.id)" + BASE_FROM + where, Long.class);
                    bind(countQuery, parameters);
                    final long total = countQuery.getSingleResult();

                    final TypedQuery<EstudianteResumenQueryRow> rows = entityManager.createQuery(
                            SELECT_RESUMEN + BASE_FROM + where + ORDER, EstudianteResumenQueryRow.class);
                    bind(rows, parameters);
                    final List<EstudianteResumenRepositoryProjection> items = rows
                            .setFirstResult(dto.page() * dto.size()).setMaxResults(dto.size()).getResultList().stream()
                            .map(CoreViewJpaProjectionMapper::toEstudianteResumen).toList();
                    return new EstudiantePaginaRepositoryProjection(
                            items, total, totalPages(total, dto.size()), dto.page(), dto.size());
                }
        );
    }

    @Override
    public Optional<EstudianteDetalleRepositoryProjection> consultarEstudiantePorId(final UUID estudianteId) {
        if (ObjectHelper.isNull(estudianteId)) {
            throw new CrosscuttingException("El identificador del estudiante es obligatorio.");
        }
        return JpaQueryExecutor.execute(
                "consultarEstudiantePorId",
                "No fue posible consultar el estudiante por ID.",
                () -> {
                    final List<EstudianteResumenRepositoryProjection> personal = entityManager.createQuery(
                                    SELECT_RESUMEN + BASE_FROM + " where e.id = :estudianteId",
                                    EstudianteResumenQueryRow.class)
                            .setParameter("estudianteId", estudianteId).setMaxResults(1).getResultList().stream()
                            .map(CoreViewJpaProjectionMapper::toEstudianteResumen).toList();
                    if (personal.isEmpty()) {
                        return Optional.<EstudianteDetalleRepositoryProjection>empty();
                    }
                    final List<EstudianteContextoAcademicoRepositoryProjection> contexts = entityManager
                            .createQuery(HQL_CONTEXTOS, UvEstudianteEntity.class)
                            .setParameter("estudianteId", estudianteId).getResultList().stream()
                            .map(CoreViewJpaProjectionMapper::toEstudianteContexto).toList();
                    return Optional.of(new EstudianteDetalleRepositoryProjection(personal.getFirst(), contexts));
                }
        );
    }

    private static String buildWhere(final ConsultarEstudiantesRepositoryDTO dto,
                                     final Map<String, Object> parameters) {
        final StringBuilder where = new StringBuilder(" where 1 = 1");
        add(where, parameters, "tipoIdentificacionId", "u.idTipoIdentificacion", dto.tipoIdentificacionId());
        add(where, parameters, "numeroIdentificacion", "u.numeroIdentificacion", dto.numeroIdentificacion());
        if (!TextHelper.isNullOrBlank(dto.nombre())) {
            where.append(" and upper(u.nombreCompleto) like :nombre");
            parameters.put("nombre", "%" + dto.nombre().toUpperCase(Locale.ROOT) + "%");
        }
        if (!TextHelper.isNullOrBlank(dto.correo())) {
            where.append(" and lower(u.correo) like :correo");
            parameters.put("correo", "%" + dto.correo().toLowerCase(Locale.ROOT) + "%");
        }
        add(where, parameters, "activo", "u.estaActivoUsuario", dto.activo());
        if (dto.institucionId() != null || dto.facultadId() != null || dto.programaId() != null || dto.grupoId() != null) {
            where.append(" and exists (select 1 from UvEstudianteEntity a where a.id = e.id");
            add(where, parameters, "institucionId", "a.idInstitucion", dto.institucionId());
            add(where, parameters, "facultadId", "a.idFacultad", dto.facultadId());
            add(where, parameters, "programaId", "a.idPrograma", dto.programaId());
            add(where, parameters, "grupoId", "a.idGrupo", dto.grupoId());
            where.append(')');
        }
        return where.toString();
    }

    private static void add(final StringBuilder where, final Map<String, Object> parameters,
                            final String parameter, final String field, final Object value) {
        if (value != null) { where.append(" and ").append(field).append(" = :").append(parameter); parameters.put(parameter, value); }
    }

    private static void bind(final Query query, final Map<String, Object> parameters) {
        parameters.forEach(query::setParameter);
    }

    private static int totalPages(final long total, final int size) {
        return total == 0 ? 0 : (int) Math.ceil((double) total / size);
    }
}
