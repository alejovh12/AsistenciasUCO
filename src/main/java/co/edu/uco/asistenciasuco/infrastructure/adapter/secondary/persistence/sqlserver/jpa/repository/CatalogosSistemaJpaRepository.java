package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.CatalogosSistemaConsultaPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CategoriaJustificacionCatalogoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.MotivoCancelacionCatalogoProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

/** Lectura de solo consulta de uv_categoria_justificacion y uv_motivo_cancelacion_sesion (catalogos definidos por el sistema). */
@Repository
public class CatalogosSistemaJpaRepository implements CatalogosSistemaConsultaPort {

    static final String SQL_CATEGORIAS = """
            SELECT c.id, c.codigo, c.nombre, c.descripcion, c.requiereSoporte FROM dbo.uv_categoria_justificacion c ORDER BY c.nombre
            """;
    static final String SQL_MOTIVOS = """
            SELECT m.id, m.codigo, m.nombre, m.orden, m.estaActivo FROM dbo.uv_motivo_cancelacion_sesion m ORDER BY m.orden, m.nombre
            """;
    static final String OP_CATEGORIAS = "consultarCatalogoCategoriasJustificacion";
    static final String OP_MOTIVOS = "consultarCatalogoMotivosCancelacion";

    private final EntityManager entityManager;

    public CatalogosSistemaJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
    }

    @Override
    public List<CategoriaJustificacionCatalogoProjection> consultarCategoriasJustificacion() {
        return JpaQueryExecutor.execute(OP_CATEGORIAS, "No fue posible consultar el catalogo de categorias de justificacion.",
                () -> ((List<?>) entityManager.createNativeQuery(SQL_CATEGORIAS).getResultList()).stream()
                        .map(Object[].class::cast)
                        .map(f -> new CategoriaJustificacionCatalogoProjection(NativeRowValues.uuid(f[0]), NativeRowValues.texto(f[1]), NativeRowValues.texto(f[2]),
                                NativeRowValues.texto(f[3]), NativeRowValues.bit(f[4])))
                        .toList());
    }

    @Override
    public List<MotivoCancelacionCatalogoProjection> consultarMotivosCancelacion() {
        return JpaQueryExecutor.execute(OP_MOTIVOS, "No fue posible consultar el catalogo de motivos de cancelacion de sesion.",
                () -> ((List<?>) entityManager.createNativeQuery(SQL_MOTIVOS).getResultList()).stream()
                        .map(Object[].class::cast)
                        .map(f -> new MotivoCancelacionCatalogoProjection(NativeRowValues.uuid(f[0]), NativeRowValues.texto(f[1]), NativeRowValues.texto(f[2]),
                                NativeRowValues.entero(f[3]), NativeRowValues.bit(f[4])))
                        .toList());
    }
}
