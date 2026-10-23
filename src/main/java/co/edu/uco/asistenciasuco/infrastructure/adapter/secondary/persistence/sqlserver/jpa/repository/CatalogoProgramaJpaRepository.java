package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.CatalogoProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CatalogoProgramaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvJornadaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvModalidadEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvTipoProgramaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;

/** Catalogos activos de tipo de programa, modalidad y jornada leidos de sus vistas. */
@Repository
public class CatalogoProgramaJpaRepository implements CatalogoProgramaQueryPort {

    static final String HQL_TIPOS = "select t from UvTipoProgramaEntity t where t.estaActivoTipoPrograma = true order by t.nombre";
    static final String HQL_MODALIDADES = "select m from UvModalidadEntity m where m.estaActivoModalidad = true order by m.nombre";
    static final String HQL_JORNADAS = "select j from UvJornadaEntity j where j.estaActivoJornada = true order by j.nombre";

    private final EntityManager entityManager;

    public CatalogoProgramaJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de catalogos de programa es obligatorio.");
    }

    @Override
    public List<CatalogoProgramaProjection> consultarTiposPrograma() {
        return JpaQueryExecutor.execute("consultarTiposPrograma", "No fue posible consultar los tipos de programa desde base de datos.",
                () -> entityManager.createQuery(HQL_TIPOS, UvTipoProgramaEntity.class).getResultList().stream()
                        .map(e -> new CatalogoProgramaProjection(e.id(), e.codigo(), e.nombre())).toList());
    }

    @Override
    public List<CatalogoProgramaProjection> consultarModalidades() {
        return JpaQueryExecutor.execute("consultarModalidadesPrograma", "No fue posible consultar las modalidades desde base de datos.",
                () -> entityManager.createQuery(HQL_MODALIDADES, UvModalidadEntity.class).getResultList().stream()
                        .map(e -> new CatalogoProgramaProjection(e.id(), e.codigo(), e.nombre())).toList());
    }

    @Override
    public List<CatalogoProgramaProjection> consultarJornadas() {
        return JpaQueryExecutor.execute("consultarJornadasPrograma", "No fue posible consultar las jornadas desde base de datos.",
                () -> entityManager.createQuery(HQL_JORNADAS, UvJornadaEntity.class).getResultList().stream()
                        .map(e -> new CatalogoProgramaProjection(e.id(), e.codigo(), e.nombre())).toList());
    }
}
