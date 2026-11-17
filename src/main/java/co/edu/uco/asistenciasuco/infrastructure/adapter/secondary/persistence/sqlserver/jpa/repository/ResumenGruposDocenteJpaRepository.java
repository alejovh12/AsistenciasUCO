package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.ResumenGruposDocenteQueryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** CIERRE-W3I: ejecuta {@code usp_consultar_resumen_grupos_docente} (totales de los grupos habilitados del docente titular). */
@Repository
public class ResumenGruposDocenteJpaRepository implements ResumenGruposDocenteQueryPort {

    static final String SQL_RESUMEN_GRUPOS = """
            EXEC dbo.usp_consultar_resumen_grupos_docente
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion
            """;

    private final EntityManager entityManager;

    public ResumenGruposDocenteJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager del resumen de grupos del docente es obligatorio.");
    }

    @Override
    @SuppressWarnings("unchecked")
    public ResumenGrupos consultarResumenGruposDocente(final UUID usuarioEjecutor) {
        final UUID correlacion = CorrelationIdContext.require();
        return JpaQueryExecutor.execute("consultarResumenGruposDocente",
                "No fue posible consultar el resumen de los grupos del docente.", () -> {
                    final var consulta = entityManager.createNativeQuery(SQL_RESUMEN_GRUPOS, Tuple.class)
                            .setParameter("idUsuarioEjecutor", usuarioEjecutor)
                            .setParameter("idCorrelacion", correlacion);
                    final List<Tuple> filas = consulta.getResultList();
                    if (filas.isEmpty()) {
                        return new ResumenGrupos(0, 0, 0, null);
                    }
                    final Tuple f = filas.getFirst();
                    return new ResumenGrupos(NativeRowValues.entero(f.get("gruposHabilitados")), NativeRowValues.entero(f.get("estudiantesActivos")),
                            NativeRowValues.entero(f.get("cupoTotal")), NativeRowValues.decimal2(f.get("tasaOcupacion")));
                }, excepcion -> DbCodeTranslator.traducir(excepcion, "grupo"));
    }
}
