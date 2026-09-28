package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.AsistenciaQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Flujo y traduccion de errores de la persistencia JPA con fakes. NO certifica que JPA/SQL Server
 * funcione: eso lo certifica {@code AsistenciaQueryJpaParityIT}.
 */
class AsistenciaJpaQueryPersistenceTest {

    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID SESION = UUID.randomUUID();

    private final EntityManagerFactory factory = mock(EntityManagerFactory.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    @SuppressWarnings("unchecked")
    private final TypedQuery<AsistenciaQueryRow> query = mock(TypedQuery.class);
    private final AsistenciaJpaQueryPersistence persistence = new AsistenciaJpaQueryPersistence(factory);

    private void wireQuery() {
        when(factory.createEntityManager()).thenReturn(entityManager);
        when(entityManager.createQuery(anyString(), eq(AsistenciaQueryRow.class))).thenReturn(query);
        when(query.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(query);
    }

    @Test
    void dto_nulo_lanza_la_misma_excepcion_semantica_que_jdbc_sin_abrir_entity_manager() {
        final CrosscuttingException exception =
                assertThrows(CrosscuttingException.class, () -> persistence.consultarAsistenciasPorGrupo(null));

        assertEquals("El dominio para consultar asistencias por grupo es obligatorio.", exception.getMessage());
        verifyNoInteractions(factory);
    }

    @Test
    void constructor_exige_entity_manager_factory() {
        assertThrows(NullPointerException.class, () -> new AsistenciaJpaQueryPersistence(null));
    }

    @Test
    void mapea_filas_pasa_parametros_grupo_y_sesion_y_cierra_el_entity_manager() {
        wireQuery();
        final UUID asistencia = UUID.randomUUID();
        final UUID estudiante = UUID.randomUUID();
        when(query.getResultList()).thenReturn(List.of(
                new AsistenciaQueryRow(asistencia, estudiante, GRUPO, SESION, null, null)));

        final List<AsistenciaRepositoryProjection> result = persistence.consultarAsistenciasPorGrupo(
                new ConsultarAsistenciasPorGrupoRepositoryDTO(GRUPO, SESION));

        assertEquals(1, result.size());
        assertEquals(asistencia, result.getFirst().getAsistencia());
        assertFalse(result.getFirst().isPresente());
        assertNull(result.getFirst().getEstado());
        assertEquals("", result.getFirst().getObservacion());
        verify(query).setParameter("grupo", GRUPO);
        verify(query).setParameter("sesion", SESION);
        verify(entityManager).close();
    }

    @Test
    void sesion_nula_se_pasa_como_parametro_nulo_no_como_otra_query() {
        wireQuery();
        when(query.getResultList()).thenReturn(List.of());

        final List<AsistenciaRepositoryProjection> result = persistence.consultarAsistenciasPorGrupo(
                new ConsultarAsistenciasPorGrupoRepositoryDTO(GRUPO, null));

        assertTrue(result.isEmpty());
        verify(query).setParameter("sesion", (Object) null);
    }

    @Test
    void hql_es_una_sola_query_no_native_sin_order_by_ni_auth() {
        final String hql = AsistenciaJpaQueryPersistence.HQL_CONSULTAR_ASISTENCIAS.toLowerCase();

        assertFalse(hql.contains("order by"), "La query no debe introducir orden observable.");
        assertFalse(hql.contains("uv_auth"));
        assertFalse(hql.contains("session_context"));
        assertTrue(hql.contains("eg.idgrupo = :grupo"));
        assertTrue(hql.contains(":sesion is null or a.idsesion = :sesion"));
    }

    @Test
    void persistence_exception_se_traduce_a_database_operation_exception_sin_filtrar_jpa() {
        wireQuery();
        final PersistenceException cause = new PersistenceException("jpa-detail");
        when(query.getResultList()).thenThrow(cause);

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> persistence.consultarAsistenciasPorGrupo(new ConsultarAsistenciasPorGrupoRepositoryDTO(GRUPO, SESION)));

        assertEquals("No fue posible consultar las asistencias de base de datos.", exception.getMessage());
        assertSame(cause, exception.getCause());
        verify(entityManager).close();
    }

    @Test
    void illegal_state_al_abrir_entity_manager_tambien_se_traduce() {
        when(factory.createEntityManager()).thenThrow(new IllegalStateException("factory closed"));

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> persistence.consultarAsistenciasPorGrupo(new ConsultarAsistenciasPorGrupoRepositoryDTO(GRUPO, null)));

        assertNotNull(exception.getCause());
    }

    @Test
    void illegal_argument_de_la_query_tambien_se_traduce() {
        wireQuery();
        when(query.getResultList()).thenThrow(new IllegalArgumentException("bad query"));

        assertThrows(DatabaseOperationException.class,
                () -> persistence.consultarAsistenciasPorGrupo(new ConsultarAsistenciasPorGrupoRepositoryDTO(GRUPO, SESION)));
    }
}
