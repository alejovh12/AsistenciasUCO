package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.PadronGrupoPaginaProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CIERRE-W1A: el padron paginado del docente ejecuta el SP con pagina, tamano y busqueda, y toma el total de la 8.a columna. */
class GrupoJpaRepositoryPadronTest {

    private static final UUID CORRELACION = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();

    private final EntityManager manager = mock(EntityManager.class);
    private final Query query = mock(Query.class);

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELACION);
        when(manager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private GrupoJpaRepository grupos() {
        return new GrupoJpaRepository(manager, mock(TransactionOperations.class), new JpaProcedureExecutor(manager));
    }

    @Test
    void ejecuta_el_sp_con_ejecutor_pagina_tamano_y_busqueda_y_toma_el_total_de_la_octava_columna() {
        final UUID matricula = UUID.randomUUID();
        final UUID estudiante = UUID.randomUUID();
        when(query.getResultList()).thenReturn(Collections.<Object>singletonList(
                new Object[]{matricula.toString(), estudiante, "1979000001", "Alfa Padron", "a@x.edu", "A", "Activo", 37}));

        final PadronGrupoPaginaProjection pagina = grupos().consultarPadronPagina(GRUPO, ACTOR, 2, 12, "alfa");

        verify(manager).createNativeQuery(GrupoJpaRepository.SQL_PADRON_GRUPO);
        verify(query).setParameter("idGrupo", GRUPO);
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
        verify(query).setParameter("idCorrelacion", CORRELACION);
        verify(query).setParameter("numeroPagina", 2);
        verify(query).setParameter("tamanoPagina", 12);
        verify(query).setParameter("busqueda", "alfa");
        assertEquals(37L, pagina.totalRegistros());
        assertEquals(1, pagina.estudiantes().size());
        assertEquals(matricula, pagina.estudiantes().get(0).id());
        assertEquals(estudiante, pagina.estudiantes().get(0).idEstudiante());
        assertEquals("1979000001", pagina.estudiantes().get(0).documento());
        assertEquals("Alfa Padron", pagina.estudiantes().get(0).nombreCompleto());
        assertEquals("A", pagina.estudiantes().get(0).codigoEstado());
    }

    @Test
    void sin_busqueda_envia_nulo_y_sin_filas_el_total_es_cero() {
        when(query.getResultList()).thenReturn(List.of());

        final PadronGrupoPaginaProjection pagina = grupos().consultarPadronPagina(GRUPO, ACTOR, 1, 10, null);

        verify(query).setParameter("busqueda", null);
        assertTrue(pagina.estudiantes().isEmpty());
        assertEquals(0L, pagina.totalRegistros());
    }

    @Test
    void sin_grupo_o_ejecutor_no_llega_a_la_db() {
        final var grupos3 = grupos();
        assertThrows(CrosscuttingException.class, () -> grupos3.consultarPadronPagina(null, ACTOR, 1, 10, null));
        final var grupos2 = grupos();
        assertThrows(CrosscuttingException.class, () -> grupos2.consultarPadronPagina(GRUPO, null, 1, 10, null));
        verify(manager, never()).createNativeQuery(anyString());
    }

    @Test
    void grupo_fuera_de_ambito_es_403() {
        when(query.getResultList()).thenThrow(new PersistenceException("x", new RuntimeException("DBCODE=SEC_002|ajeno")));

        final var grupos2 = grupos();
        assertThrows(ForbiddenException.class, () -> grupos2.consultarPadronPagina(GRUPO, ACTOR, 1, 10, "a"));
    }

    @Test
    void una_fila_con_otra_cantidad_de_columnas_falla() {
        when(query.getResultList()).thenReturn(Collections.<Object>singletonList(new Object[7]));

        final var grupos2 = grupos();
        assertThrows(DatabaseOperationException.class, () -> grupos2.consultarPadronPagina(GRUPO, ACTOR, 1, 10, null));
    }
}
