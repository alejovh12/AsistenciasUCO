package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.DocenteRepositoryPort;
import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.AsignarDocenteAGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsignacionesAcademicasDocenteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarDocentePorIdRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarDocenteDesdeUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.DocenteOperacionRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import org.springframework.stereotype.Repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.DocenteAsignacionAcademicaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.DocenteIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.CoreViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvDocenteEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvDocenteIdentidadEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DocenteJpaRepository implements DocenteRepositoryPort {
    private static final String SELECT_IDENTIDAD = """
            select d
            from UvDocenteIdentidadEntity d
            """;
    static final String HQL_TODOS = SELECT_IDENTIDAD + " order by d.nombreCompleto, d.id";
    static final String HQL_POR_ID = SELECT_IDENTIDAD + " where d.id = :idDocente";
    static final String HQL_ASIGNACIONES = """
            select d
            from UvDocenteEntity d
            where d.id = :idDocente
            order by d.nombreInstitucion, d.nombreFacultad, d.nombrePrograma,
                d.nombreAsignatura, d.nombreGrupo, d.idGrupo
            """;
    private final EntityManager entityManager;

    public DocenteJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de consultas de docente es obligatorio.");
    }

    @Override
    public List<DocenteIdentidadRepositoryProjection> consultarDocentes() {
        return JpaQueryExecutor.execute(
                "consultarDocentes",
                "No fue posible consultar los docentes.",
                () -> entityManager.createQuery(HQL_TODOS, UvDocenteIdentidadEntity.class).getResultList().stream()
                        .map(CoreViewJpaProjectionMapper::toDocenteIdentidad).toList()
        );
    }

    private Optional<DocenteIdentidadRepositoryProjection> consultarDocentePorIdInterno(final UUID docenteId) {
        return JpaQueryExecutor.execute(
                "consultarDocentePorId",
                "No fue posible consultar el docente por ID.",
                () -> entityManager.createQuery(HQL_POR_ID, UvDocenteIdentidadEntity.class)
                        .setParameter("idDocente", docenteId).setMaxResults(1).getResultList().stream().findFirst()
                        .map(CoreViewJpaProjectionMapper::toDocenteIdentidad)
        );
    }

    private List<DocenteAsignacionAcademicaRepositoryProjection> consultarAsignaciones(final UUID docenteId) {
        return JpaQueryExecutor.execute(
                "consultarAsignacionesAcademicas",
                "No fue posible consultar las asignaciones academicas del docente.",
                () -> entityManager.createQuery(HQL_ASIGNACIONES, UvDocenteEntity.class)
                        .setParameter("idDocente", docenteId).getResultList().stream()
                        .map(CoreViewJpaProjectionMapper::toDocenteAsignacion).toList()
        );
    }

    @Override
    public Optional<DocenteIdentidadRepositoryProjection> consultarDocentePorId(
            final ConsultarDocentePorIdRepositoryDTO dto) {
        require(dto, "El DTO para consultar docente por ID es obligatorio.");
        return consultarDocentePorIdInterno(dto.getDocente());
    }

    @Override
    public List<DocenteAsignacionAcademicaRepositoryProjection> consultarAsignacionesAcademicas(
            final ConsultarAsignacionesAcademicasDocenteRepositoryDTO dto) {
        require(dto, "El DTO para consultar asignaciones academicas del docente es obligatorio.");
        return consultarAsignaciones(dto.getDocente());
    }

    @Override
    public DocenteOperacionRepositoryProjection registrarDocenteDesdeUsuario(
            final RegistrarDocenteDesdeUsuarioRepositoryDTO dto) {
        require(dto, "El DTO para registrar docente desde usuario es obligatorio.");
        throw new FeatureUnavailableException(
                "El registro de docente desde usuario requiere un command publico de DB; los procedimientos internos no estan permitidos.");
    }

    @Override
    public DocenteOperacionRepositoryProjection asignarDocenteAGrupo(final AsignarDocenteAGrupoRepositoryDTO dto) {
        require(dto, "El DTO para asignar docente a grupo es obligatorio.");
        throw new FeatureUnavailableException(
                "La asignacion de docente a grupo requiere un command publico de DB; los procedimientos internos no estan permitidos.");
    }

    private static void require(final Object value, final String message) {
        if (ObjectHelper.isNull(value)) throw new CrosscuttingException(message);
    }
}

