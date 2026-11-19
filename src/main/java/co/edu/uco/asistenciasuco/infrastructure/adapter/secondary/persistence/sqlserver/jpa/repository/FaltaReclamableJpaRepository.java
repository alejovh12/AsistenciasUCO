package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.FaltaReclamableRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * ATA D10 (HU026B): faltas por reclamar del estudiante via {@code usp_consultar_faltas_reclamables_estudiante} (JPA, {@code EXEC} nativo,
 * solo lectura). El SP devuelve 14 columnas: {@code idEstudianteGrupo} (que no se expone), las 12 de la fila y {@code totalRegistros}. El
 * perfil ({@code SEC_001}) y la pagina ({@code PAG_001}) los valida la DB.
 */
@Repository
public class FaltaReclamableJpaRepository implements FaltaReclamableRepositoryPort {

    static final String SQL_ESTUDIANTE = """
            EXEC dbo.usp_consultar_faltas_reclamables_estudiante
                 @idUsuarioEjecutor = :idUsuarioEjecutor, @idCorrelacion = :idCorrelacion,
                 @numeroPagina = :numeroPagina, @tamanoPagina = :tamanoPagina
            """;
    /** idEstudianteGrupo, idGrupo, nombreGrupo, idAsignatura, nombreAsignatura, idSesion, numeroSesion, inicio, fin, idToma, numeroToma, punto, limite, total. */
    static final int COLUMNAS_FALTA_RECLAMABLE = 14;

    private final EntityManager entityManager;

    public FaltaReclamableJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de las faltas reclamables es obligatorio.");
    }

    @Override
    public PaginaFaltasReclamables consultarEstudiante(final UUID usuarioId, final int pagina, final int tamano) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                "consultarFaltasReclamablesEstudiante",
                "No fue posible consultar las faltas por reclamar del estudiante.",
                () -> aPagina((List<?>) entityManager.createNativeQuery(SQL_ESTUDIANTE)
                        .setParameter("idUsuarioEjecutor", usuarioId)
                        .setParameter("idCorrelacion", correlationId)
                        .setParameter("numeroPagina", pagina)
                        .setParameter("tamanoPagina", tamano)
                        .getResultList()),
                DbCodeTranslator::traducir
        );
    }

    private static PaginaFaltasReclamables aPagina(final List<?> filas) {
        long total = 0;
        final List<FilaFaltaReclamable> resultado = new ArrayList<>(filas.size());
        for (final Object fila : filas) {
            final Object[] f = (Object[]) fila;
            if (f.length != COLUMNAS_FALTA_RECLAMABLE) {
                throw new IllegalStateException("El SP de faltas reclamables devolvio " + f.length + " columnas y se esperaban "
                        + COLUMNAS_FALTA_RECLAMABLE + ".");
            }
            total = Math.max(total, NativeRowValues.entero(f[13]));
            resultado.add(new FilaFaltaReclamable(
                    NativeRowValues.uuid(f[1]), NativeRowValues.texto(f[2]), NativeRowValues.uuid(f[3]), NativeRowValues.texto(f[4]),
                    NativeRowValues.uuid(f[5]), NativeRowValues.entero(f[6]), NativeRowValues.instanteUtc(f[7]), NativeRowValues.instanteUtc(f[8]),
                    NativeRowValues.uuid(f[9]), NativeRowValues.entero(f[10]), NativeRowValues.instanteUtc(f[11]), NativeRowValues.instanteUtc(f[12])));
        }
        return new PaginaFaltasReclamables(resultado, total);
    }
}
