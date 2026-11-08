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
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaEstudiantesProgramaProjection;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;

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

    /** CULM-001 B2NM: columnas del SP en modo listado: id, idUsuario, tipoIdentificacionId, numeroIdentificacion, primerApellido, segundoApellido, primerNombre, segundoNombre, nombreCompleto, correo, estaActivoUsuario, totalRegistros. */
    static final String SQL_DIRECTORIO = """
            EXEC dbo.usp_consultar_directorio_estudiantes_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @idPrograma = :idPrograma,
                 @nombre = :nombre,
                 @activo = :activo,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina
            """;
    static final int COLUMNAS_DIRECTORIO = 12;

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

    @Override
    public PaginaEstudiantesProgramaProjection consultarDirectorioPrograma(final UUID actorUsuarioId, final UUID idPrograma, final String busqueda,
                                                                         final Boolean estaActivo, final int pagina, final int tamano) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarDirectorioEstudiantesPrograma", "No fue posible consultar los estudiantes del programa.", () -> {
            final java.util.List<?> filas;
            try {
                filas = entityManager.createNativeQuery(SQL_DIRECTORIO)
                        .setParameter("idUsuarioEjecutor", actorUsuarioId).setParameter("idCorrelacion", correlationId)
                        .setParameter("idPrograma", idPrograma).setParameter("nombre", busqueda).setParameter("activo", estaActivo)
                        .setParameter("numeroPagina", pagina).setParameter("tamanoPagina", tamano).getResultList();
            } catch (final jakarta.persistence.PersistenceException exception) {
                throw DbCodeTranslator.traducir(exception);
            }
            long total = 0;
            final java.util.List<EstudianteProgramaProjection> lista = new java.util.ArrayList<>();
            for (final Object fila : filas) {
                final Object[] f = (Object[]) fila;
                if (f.length != COLUMNAS_DIRECTORIO) {
                    throw new IllegalStateException("usp_consultar_directorio_estudiantes_paginado debe devolver " + COLUMNAS_DIRECTORIO + " columnas.");
                }
                lista.add(new EstudianteProgramaProjection(uuid(f[0]), uuid(f[1]), f[3] == null ? null : String.valueOf(f[3]), f[8] == null ? null : f[8].toString(),
                        f[9] == null ? null : f[9].toString(), idPrograma, null));
                total = ((Number) f[11]).longValue();
            }
            return new PaginaEstudiantesProgramaProjection(lista, total);
        });
    }

    private static UUID uuid(final Object valor) {
        return valor == null ? null : (valor instanceof UUID u ? u : UUID.fromString(valor.toString()));
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
