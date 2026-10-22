package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CatalogoProgramaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvJornadaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvModalidadEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvTipoProgramaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** T-023-01: catalogos activos de tipo de programa, modalidad y jornada leidos de sus vistas. */
class CatalogoProgramaJpaRepositoryTest {

    private static final UUID ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final CatalogoProgramaJpaRepository repository = new CatalogoProgramaJpaRepository(entityManager);

    @SuppressWarnings("unchecked")
    private <T> TypedQuery<T> consulta(final Class<T> tipo, final List<T> filas) {
        final TypedQuery<T> typed = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(tipo))).thenReturn(typed);
        when(typed.getResultList()).thenReturn(filas);
        return typed;
    }

    @Test
    void tipos_de_programa_activos_se_mapean_con_codigo_y_nombre() {
        final UvTipoProgramaEntity fila = mock(UvTipoProgramaEntity.class);
        when(fila.id()).thenReturn(ID);
        when(fila.codigo()).thenReturn("PRE");
        when(fila.nombre()).thenReturn("pregrado");
        consulta(UvTipoProgramaEntity.class, List.of(fila));

        assertEquals(List.of(new CatalogoProgramaProjection(ID, "PRE", "pregrado")), repository.consultarTiposPrograma());
        verify(entityManager).createQuery(contains("estaActivoTipoPrograma = true"), eq(UvTipoProgramaEntity.class));
    }

    @Test
    void modalidades_activas_se_mapean() {
        final UvModalidadEntity fila = mock(UvModalidadEntity.class);
        when(fila.id()).thenReturn(ID);
        when(fila.codigo()).thenReturn("VIR");
        when(fila.nombre()).thenReturn("Virtual");
        consulta(UvModalidadEntity.class, List.of(fila));

        assertEquals(List.of(new CatalogoProgramaProjection(ID, "VIR", "Virtual")), repository.consultarModalidades());
        verify(entityManager).createQuery(contains("estaActivoModalidad = true"), eq(UvModalidadEntity.class));
    }

    @Test
    void jornadas_activas_se_mapean() {
        final UvJornadaEntity fila = mock(UvJornadaEntity.class);
        when(fila.id()).thenReturn(ID);
        when(fila.codigo()).thenReturn("NOC");
        when(fila.nombre()).thenReturn("Nocturna");
        consulta(UvJornadaEntity.class, List.of(fila));

        assertEquals(List.of(new CatalogoProgramaProjection(ID, "NOC", "Nocturna")), repository.consultarJornadas());
        verify(entityManager).createQuery(contains("estaActivoJornada = true"), eq(UvJornadaEntity.class));
    }

    @Test
    void el_entity_manager_es_obligatorio() {
        assertThrows(NullPointerException.class, () -> new CatalogoProgramaJpaRepository(null));
    }
}
