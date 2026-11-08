package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConfiguracionReclamosQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ConfiguracionReclamosProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Configuracion del formulario de reclamos (CULM-001 D.2) por {@code usp_consultar_configuracion_reclamos}: una fila con
 * maximoSoportes, tamanioMaximoBytes, tiposPermitidos, limiteSubidasPorMinuto y diasLimiteJustificacion. El rol (ESTUDIANTE o
 * DOCENTE) lo valida el SP (THROW 51000 con DBCODE, traducido por {@link DbCodeTranslator}).
 */
@Repository
public class ConfiguracionReclamosJpaRepository implements ConfiguracionReclamosQueryPort {

    static final String OP_CONSULTAR = "consultarConfiguracionReclamos";
    static final String SQL_CONSULTAR = """
            EXEC dbo.usp_consultar_configuracion_reclamos
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion
            """;
    private static final int COLUMNAS = 5;

    private final EntityManager entityManager;

    public ConfiguracionReclamosJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
    }

    @Override
    public ConfiguracionReclamosProjection consultar(final UUID usuarioEjecutor) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                OP_CONSULTAR,
                "No fue posible consultar la configuracion de reclamos desde base de datos.",
                () -> {
                    final List<?> filas = entityManager.createNativeQuery(SQL_CONSULTAR)
                            .setParameter("idUsuarioEjecutor", usuarioEjecutor)
                            .setParameter("idCorrelacion", correlationId)
                            .getResultList();
                    return filas.isEmpty() ? null : aProyeccion((Object[]) filas.getFirst());
                },
                DbCodeTranslator::traducir
        );
    }

    private static ConfiguracionReclamosProjection aProyeccion(final Object[] fila) {
        if (fila.length != COLUMNAS) {
            throw new IllegalStateException("La configuracion de reclamos devolvio " + fila.length + " columnas; se esperaban " + COLUMNAS + ".");
        }
        return new ConfiguracionReclamosProjection(entero(fila[0]), largo(fila[1]), fila[2] == null ? null : fila[2].toString(),
                entero(fila[3]), entero(fila[4]));
    }

    private static Integer entero(final Object valor) {
        return valor == null ? null : ((Number) valor).intValue();
    }

    private static Long largo(final Object valor) {
        return valor == null ? null : ((Number) valor).longValue();
    }
}
