package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** IDENT-002: el reenvio de activacion invoca usp_consultar_reenvio_activacion_cuenta con actor y correlacion. */
class VerificacionCorreoJpaRepositoryReenvioActivacionTest {

    private static final UUID USUARIO = UUID.fromString("23641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID ACTOR = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID CORRELACION = UUID.fromString("33641bab-e3cd-485c-b275-47e7b731e18c");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private final VerificacionCorreoJpaRepository repositorio =
            new VerificacionCorreoJpaRepository(entityManager, mock(JpaProcedureExecutor.class));

    @BeforeEach
    void setUp() {
        CorrelationIdContext.set(CORRELACION);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(query);
    }

    @AfterEach
    void tearDown() {
        CorrelationIdContext.clear();
    }

    @Test
    void devuelve_el_usuario_validado_por_el_sp_con_los_parametros_del_contrato() {
        when(query.getResultList()).thenReturn(List.of(USUARIO.toString()));

        assertEquals(USUARIO, repositorio.consultarReenvioActivacion(USUARIO, ACTOR));

        verify(entityManager).createNativeQuery(VerificacionCorreoJpaRepository.SQL_REENVIO_ACTIVACION);
        verify(query).setParameter("idUsuario", USUARIO);
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
        verify(query).setParameter("idCorrelacion", CORRELACION);
    }

    @Test
    void el_dbcode_act_002_se_traduce_a_conflicto() {
        when(query.getResultList()).thenThrow(
                new PersistenceException("wrapper", new RuntimeException("DBCODE=ACT_002|cuenta activa")));

        final ConflictException error = assertThrows(ConflictException.class,
                () -> repositorio.consultarReenvioActivacion(USUARIO, ACTOR));
        assertEquals("ACT_002", error.getCode());
    }

    @Test
    void sin_filas_es_un_fallo_tecnico() {
        when(query.getResultList()).thenReturn(List.of());

        assertThrows(RuntimeException.class, () -> repositorio.consultarReenvioActivacion(USUARIO, ACTOR));
    }
}
