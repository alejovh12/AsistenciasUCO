package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvPeriodoAcademicoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.HydratedViewRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CIERRE-W2A: adaptadores JPA del estado del periodo y de las opciones del consolidado. */
class CierreW2aJpaRepositoriesTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID EJECUTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PERIODO = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ASIGNATURA = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID DOCENTE = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private final EntityManager entityManager = mock(EntityManager.class);

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    void los_periodos_traen_el_estado_de_la_vista_y_se_filtran_por_estado_en_la_base() {
        final UvPeriodoAcademicoEntity activo = HydratedViewRow.of(UvPeriodoAcademicoEntity.class, Map.of(
                "id", PERIODO, "nombre", "2026-2", "codigo", 20262, "fechaInicio", LocalDate.of(2026, 7, 1),
                "fechaFin", LocalDate.of(2026, 12, 1), "estado", "ACTIVO"));
        final TypedQuery<UvPeriodoAcademicoEntity> porEstado = mock(TypedQuery.class);
        when(entityManager.createQuery(PeriodoAcademicoJpaRepository.HQL_LISTAR_POR_ESTADO, UvPeriodoAcademicoEntity.class)).thenReturn(porEstado);
        when(porEstado.setParameter(anyString(), any())).thenReturn(porEstado);
        when(porEstado.getResultList()).thenReturn(List.of(activo));
        final TypedQuery<UvPeriodoAcademicoEntity> todos = mock(TypedQuery.class);
        when(entityManager.createQuery(PeriodoAcademicoJpaRepository.HQL_LISTAR, UvPeriodoAcademicoEntity.class)).thenReturn(todos);
        when(todos.getResultList()).thenReturn(List.of(activo));
        final PeriodoAcademicoJpaRepository repository = new PeriodoAcademicoJpaRepository(entityManager);

        final var filtrados = repository.consultarPeriodosAcademicosPorEstado("ACTIVO");

        assertEquals("ACTIVO", filtrados.getFirst().estado());
        verify(porEstado).setParameter("estado", "ACTIVO");
        assertTrue(PeriodoAcademicoJpaRepository.HQL_LISTAR_POR_ESTADO.contains("p.estado = :estado"));
        assertEquals("ACTIVO", repository.consultarPeriodosAcademicos().getFirst().estado());
    }

    @Test
    void un_fallo_de_la_base_al_filtrar_periodos_no_devuelve_lista_vacia() {
        when(entityManager.createQuery(anyString(), any(Class.class))).thenThrow(new PersistenceException("db down"));

        assertThrows(DatabaseOperationException.class,
                () -> new PeriodoAcademicoJpaRepository(entityManager).consultarPeriodosAcademicosPorEstado("ACTIVO"));
    }

    @Test
    void las_opciones_del_consolidado_ejecutan_los_dos_sp_con_el_ejecutor_y_el_periodo() {
        final Query asignaturas = mock(Query.class);
        final Query docentes = mock(Query.class);
        when(entityManager.createNativeQuery(contains("usp_consultar_asignaturas_ambito_consolidado"))).thenReturn(asignaturas);
        when(entityManager.createNativeQuery(contains("usp_consultar_docentes_ambito_consolidado"))).thenReturn(docentes);
        for (final Query q : List.of(asignaturas, docentes)) {
            when(q.setParameter(anyString(), any())).thenReturn(q);
        }
        when(asignaturas.getResultList()).thenReturn(Collections.singletonList(new Object[]{ASIGNATURA.toString(), "Calculo"}));
        when(docentes.getResultList()).thenReturn(Collections.singletonList(new Object[]{DOCENTE.toString().toUpperCase(), "  Ana Perez "}));

        final var opciones = new ConsultasTransversalesJpaRepository(entityManager).consultarOpcionesConsolidado(EJECUTOR, PERIODO);

        assertEquals(ASIGNATURA, opciones.asignaturas().getFirst().id());
        assertEquals("Calculo", opciones.asignaturas().getFirst().nombre());
        assertEquals(DOCENTE, opciones.docentes().getFirst().id());
        assertEquals("Ana Perez", opciones.docentes().getFirst().nombre());
        for (final Query q : List.of(asignaturas, docentes)) {
            verify(q).setParameter("idUsuarioEjecutor", EJECUTOR);
            verify(q).setParameter("idCorrelacion", CORRELATION);
            verify(q).setParameter("idPeriodoAcademico", PERIODO);
        }
    }

    @Test
    void sin_opciones_las_listas_salen_vacias_y_sin_periodo_se_pasa_nulo() {
        final Query vacia = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(vacia);
        when(vacia.setParameter(anyString(), any())).thenReturn(vacia);
        when(vacia.getResultList()).thenReturn(Collections.emptyList());

        final var opciones = new ConsultasTransversalesJpaRepository(entityManager).consultarOpcionesConsolidado(EJECUTOR, null);

        assertTrue(opciones.asignaturas().isEmpty());
        assertTrue(opciones.docentes().isEmpty());
        verify(vacia, org.mockito.Mockito.atLeastOnce()).setParameter("idPeriodoAcademico", null);
    }

    @Test
    void un_perfil_sin_permiso_se_traduce_a_403() {
        final Query rechazada = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(rechazada);
        when(rechazada.setParameter(anyString(), any())).thenReturn(rechazada);
        when(rechazada.getResultList()).thenThrow(new PersistenceException("DBCODE=SEC_001|El perfil no tiene permiso"));

        assertThrows(ForbiddenException.class,
                () -> new ConsultasTransversalesJpaRepository(entityManager).consultarOpcionesConsolidado(EJECUTOR, PERIODO));
    }
}
