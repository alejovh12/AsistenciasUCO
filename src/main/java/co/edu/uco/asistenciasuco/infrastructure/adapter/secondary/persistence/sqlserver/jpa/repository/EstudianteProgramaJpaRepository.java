package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.EstudianteProgramaQueryPort;
import org.springframework.stereotype.Repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.EstudianteProgramaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.EstudianteProgramaQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Consulta JPA-only de estudiantes por programa sobre {@code uv_estudiante_programa} (LB-008 JPA-05).
 * Preserva el SQL JDBC previo: mismas columnas, mismo filtro {@code idPrograma} y mismo orden
 * ({@code nombreCompleto}, {@code id}). La identidad de la fila es {@code EstudiantePrograma.id}.
 */
@Repository
public class EstudianteProgramaJpaRepository implements EstudianteProgramaQueryPort {

    static final String JPQL_ESTUDIANTES_POR_PROGRAMA = """
            select new co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.EstudianteProgramaQueryRow(
                ep.id, ei.idUsuario, ei.numeroIdentificacion, ei.nombreCompleto, u.correo, ep.idPrograma, ep.nombrePrograma)
            from UvEstudianteProgramaEntity ep, UvEstudianteIdentidadEntity ei, UvUsuarioEntity u
            where ei.id = ep.idEstudiante
              and u.id = ei.idUsuario
              and ep.idPrograma = :idPrograma
            order by ei.nombreCompleto, ep.id
            """;

    private final EntityManager entityManager;

    public EstudianteProgramaJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de estudiantes por programa es obligatorio.");
    }

    @Override
    public List<EstudianteProgramaProjection> consultarEstudiantesPorPrograma(final UUID idPrograma) {
        return JpaQueryExecutor.execute(
                "consultarEstudiantesPorPrograma",
                "No fue posible consultar los estudiantes del programa.",
                () -> entityManager.createQuery(JPQL_ESTUDIANTES_POR_PROGRAMA, EstudianteProgramaQueryRow.class)
                        .setParameter("idPrograma", idPrograma)
                        .getResultList()
                        .stream()
                        .map(EstudianteProgramaJpaRepository::toProjection)
                        .toList()
        );
    }

    private static EstudianteProgramaProjection toProjection(final EstudianteProgramaQueryRow row) {
        return new EstudianteProgramaProjection(
                row.id(),
                row.idUsuario(),
                row.numeroIdentificacion() == null ? null : String.valueOf(row.numeroIdentificacion()),
                row.nombreCompleto(),
                row.correo(),
                row.idPrograma(),
                row.nombrePrograma());
    }
}
