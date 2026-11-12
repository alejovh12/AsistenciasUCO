package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.CoreViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvUsuarioEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import jakarta.persistence.EntityManager;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class UsuarioJpaRepository implements UsuarioRepositoryPort {

    // VAL ROL-05 (2026-11-12): se retiro el alta generica (EXEC dbo.usp_sincronizar_usuario) junto con POST /api/v1/usuarios;
    // este adaptador queda solo de consulta.

    private static final String SELECT_ROW = """
            select u
            from UvUsuarioEntity u
            """;
    static final String HQL_POR_CORREO = SELECT_ROW
            + " where lower(trim(u.correo)) = lower(trim(:correo)) order by u.id";
    static final String HQL_POR_ID = SELECT_ROW + " where u.id = :idUsuario order by u.id";
    static final String HQL_POR_IDENTIFICACION = SELECT_ROW
            + " where u.idTipoIdentificacion = :tipoIdentificacionId"
            + " and u.numeroIdentificacion = :numeroIdentificacion order by u.id";
    static final String HQL_POR_NUMERO_IDENTIFICACION = SELECT_ROW
            + " where u.numeroIdentificacion = :numeroIdentificacion order by u.id";

    // CULM-001 A.2-15: correo real del estudiante ya resuelto por usp_buscar_estudiante_por_documento_grupo (uso interno).
    static final String HQL_CORREO_POR_ESTUDIANTE = """
            select u.correo
            from UvEstudianteIdentidadEntity e join UvUsuarioEntity u on e.idUsuario = u.id
            where e.id = :idEstudiante
            """;

    private static final String PARAM_NUMERO_IDENTIFICACION = "numeroIdentificacion";

    private final EntityManager entityManager;

    public UsuarioJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
    }

    // QUERIES

    @Override
    public Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorCorreo(final String correo) {
        return consultarPorCorreo(correo);
    }

    @Override
    public Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorId(final UUID idUsuario) {
        return consultarPorId(idUsuario);
    }

    @Override
    public Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorIdentificacion(
            final UUID tipoIdentificacionId, final Integer numeroIdentificacion) {
        return consultarPorIdentificacion(tipoIdentificacionId, numeroIdentificacion);
    }

    @Override
    public Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorNumeroIdentificacion(
            final Integer numeroIdentificacion) {
        return first(HQL_POR_NUMERO_IDENTIFICACION,
                query -> query.setParameter(PARAM_NUMERO_IDENTIFICACION, numeroIdentificacion),
                "consultarUsuarioPorNumeroIdentificacion",
                "No fue posible consultar el usuario por numero de identificacion.");
    }

    private Optional<UsuarioIdentidadRepositoryProjection> consultarPorCorreo(final String correo) {
        return first(HQL_POR_CORREO, query -> query.setParameter("correo", correo), "consultarUsuarioPorCorreo",
                "No fue posible consultar el usuario por correo.");
    }

    private Optional<UsuarioIdentidadRepositoryProjection> consultarPorId(final UUID idUsuario) {
        return first(HQL_POR_ID, query -> query.setParameter("idUsuario", idUsuario), "consultarUsuarioPorId",
                "No fue posible consultar el usuario por id.");
    }

    private Optional<UsuarioIdentidadRepositoryProjection> consultarPorIdentificacion(
            final UUID tipoIdentificacionId, final Integer numeroIdentificacion) {
        return first(HQL_POR_IDENTIFICACION, query -> query.setParameter("tipoIdentificacionId", tipoIdentificacionId)
                        .setParameter(PARAM_NUMERO_IDENTIFICACION, numeroIdentificacion),
                "consultarUsuarioPorIdentificacion", "No fue posible consultar el usuario por identificacion.");
    }

    @Override
    public Optional<String> consultarCorreoPorEstudiante(final UUID idEstudiante) {
        if (idEstudiante == null) {
            return Optional.empty();
        }
        return JpaQueryExecutor.execute(
                "consultarCorreoPorEstudiante",
                "No fue posible consultar el correo del estudiante.",
                () -> entityManager.createQuery(HQL_CORREO_POR_ESTUDIANTE, String.class)
                        .setParameter("idEstudiante", idEstudiante)
                        .setMaxResults(1)
                        .getResultList().stream()
                        .filter(correo -> correo != null && !correo.isBlank())
                        .findFirst()
        );
    }

    // PRIVATE HELPERS

    private Optional<UsuarioIdentidadRepositoryProjection> first(final String hql,
            final java.util.function.UnaryOperator<jakarta.persistence.TypedQuery<UvUsuarioEntity>> binder,
            final String operation, final String message) {
        return JpaQueryExecutor.execute(
                operation,
                message,
                () -> binder.apply(entityManager.createQuery(hql, UvUsuarioEntity.class)).setMaxResults(1)
                        .getResultList().stream().findFirst().map(CoreViewJpaProjectionMapper::toUsuario)
        );
    }
}
