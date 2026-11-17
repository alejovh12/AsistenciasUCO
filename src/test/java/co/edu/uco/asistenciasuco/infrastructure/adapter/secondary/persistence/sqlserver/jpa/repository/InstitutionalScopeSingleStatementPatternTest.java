package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.*;


import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LB-008 JPA-05 AUTHORIZATION: patron de una sola sentencia por decision de ownership (docente y estudiante).
 * Una decision de autorizacion = una sentencia nativa; sin lectura previa de id (ventana TOCTOU eliminada).
 * Paridad real contra SQL Server: {@code AuthorizationReportJpaParityIT}.
 */
class InstitutionalScopeSingleStatementPatternTest {

    private static final UUID USUARIO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GRUPO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private InstitutionalScopeJpaRepository persistence;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        persistence = new InstitutionalScopeJpaRepository(entityManager);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void docente_usa_una_sola_sentencia_nativa_con_top_1_original() {
        when(query.getSingleResult()).thenReturn(1);

        assertTrue(persistence.canDocenteAccessGrupo(USUARIO, GRUPO));

        verify(entityManager, times(1)).createNativeQuery(InstitutionalScopeJpaRepository.SQL_DOCENTE_ACCEDE_GRUPO);
        verify(entityManager, never()).createQuery(anyString());
        verify(entityManager, never()).createQuery(anyString(), any(Class.class));
        assertTrue(InstitutionalScopeJpaRepository.SQL_DOCENTE_ACCEDE_GRUPO
                .contains("SELECT TOP 1 id FROM dbo.uv_docente_identidad"));
    }

    @Test
    void estudiante_usa_una_sola_sentencia_nativa_con_top_1_original() {
        when(query.getSingleResult()).thenReturn(1);

        assertTrue(persistence.canEstudianteAccessGrupo(USUARIO, GRUPO));

        verify(entityManager, times(1)).createNativeQuery(InstitutionalScopeJpaRepository.SQL_ESTUDIANTE_ACCEDE_GRUPO);
        verify(entityManager, never()).createQuery(anyString());
        verify(entityManager, never()).createQuery(anyString(), any(Class.class));
        assertTrue(InstitutionalScopeJpaRepository.SQL_ESTUDIANTE_ACCEDE_GRUPO
                .contains("SELECT TOP 1 id FROM dbo.uv_estudiante_identidad"));
    }

    @Test
    void conteo_cero_significa_denegado() {
        when(query.getSingleResult()).thenReturn(0);

        assertFalse(persistence.canDocenteAccessGrupo(USUARIO, GRUPO));
        assertFalse(persistence.canEstudianteAccessGrupo(USUARIO, GRUPO));
    }

    @Test
    void conteo_nulo_no_se_considera_acceso() {
        when(query.getSingleResult()).thenReturn(null);

        assertFalse(persistence.canEstudianteAccessGrupo(USUARIO, GRUPO));
    }

    @Test
    void fallo_tecnico_de_persistencia_se_traduce_a_database_operation_exception_conservando_causa() {
        final PersistenceException causa = new PersistenceException("fallo de base de datos");
        when(entityManager.createNativeQuery(anyString())).thenThrow(causa);

        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> persistence.canDocenteAccessGrupo(USUARIO, GRUPO));
        assertSame(causa, exception.getCause());
    }
}




