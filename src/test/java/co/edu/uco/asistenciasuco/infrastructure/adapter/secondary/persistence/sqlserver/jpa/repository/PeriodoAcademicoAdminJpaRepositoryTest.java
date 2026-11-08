package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CULM-001 D.9-a (F-ADM-030): consulta paginada de periodos del administrador por usp_consultar_periodos_academicos_paginado. */
class PeriodoAcademicoAdminJpaRepositoryTest {

    private static final UUID CORRELATION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ADMIN = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PERIODO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID INSTITUCION = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private PeriodoAcademicoJpaRepository repository;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        repository = new PeriodoAcademicoJpaRepository(entityManager);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void mapea_las_filas_con_esVigente_y_el_total() {
        when(query.getResultList()).thenReturn(List.<Object>of(
                new Object[]{PERIODO, INSTITUCION, "UCO", "2026-2", 20262, java.sql.Date.valueOf("2026-08-01"),
                        LocalDate.of(2026, 12, 15), 2026, Boolean.TRUE, 2},
                new Object[]{UUID.randomUUID(), INSTITUCION, "UCO", "2026-1", 20261, "2026-02-01", "2026-06-30", 2026, 0, 2}));

        final var pagina = repository.consultarPeriodosAdministrador(ADMIN, 1, 10);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_periodos_academicos_paginado"));
        verify(query).setParameter("idUsuarioEjecutor", ADMIN);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("numeroPagina", 1);
        verify(query).setParameter("tamanoPagina", 10);
        assertEquals(2, pagina.total());
        assertEquals(2, pagina.elementos().size());
        final var vigente = pagina.elementos().getFirst();
        assertEquals(PERIODO, vigente.id());
        assertEquals("20262", vigente.codigo());
        assertEquals(LocalDate.of(2026, 8, 1), vigente.fechaInicio());
        assertEquals(LocalDate.of(2026, 12, 15), vigente.fechaFin());
        assertTrue(vigente.esVigente());
        assertFalse(pagina.elementos().get(1).esVigente());
        assertEquals(LocalDate.of(2026, 2, 1), pagina.elementos().get(1).fechaInicio());
    }

    @Test
    void sin_filas_devuelve_pagina_vacia() {
        when(query.getResultList()).thenReturn(List.of());

        final var pagina = repository.consultarPeriodosAdministrador(ADMIN, 1, 10);

        assertEquals(0, pagina.total());
        assertTrue(pagina.elementos().isEmpty());
    }

    @Test
    void una_fila_con_columnas_distintas_a_las_esperadas_es_un_fallo_tecnico() {
        when(query.getResultList()).thenReturn(List.<Object>of(new Object[]{PERIODO}));

        assertThrows(RuntimeException.class, () -> repository.consultarPeriodosAdministrador(ADMIN, 1, 10));
    }

    @Test
    void traduce_sec_001_a_403_y_pag_001_a_400() {
        when(query.getResultList()).thenThrow(new PersistenceException("DBCODE=SEC_001|rbac"));
        assertThrows(ForbiddenException.class, () -> repository.consultarPeriodosAdministrador(ADMIN, 1, 10));

        org.mockito.Mockito.doThrow(new PersistenceException("DBCODE=PAG_001|pagina")).when(query).getResultList();
        assertThrows(ValidationException.class, () -> repository.consultarPeriodosAdministrador(ADMIN, 0, 10));
    }
}
