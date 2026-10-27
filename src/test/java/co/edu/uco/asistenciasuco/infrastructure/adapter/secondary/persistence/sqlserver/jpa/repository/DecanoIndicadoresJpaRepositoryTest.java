package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ComparativaProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.KpisDecanoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaAforoGruposProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpAforoGrupoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpComparativaProgramaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpKpisDecanoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** HU102-HU144: adapter JPA de KPIs del decano (EXEC nativo mapeado fielmente, sin recalcular). */
class DecanoIndicadoresJpaRepositoryTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FACULTAD = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PERIODO = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PROGRAMA = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID GRUPO = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID ASIGNATURA = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private DecanoIndicadoresJpaRepository repository;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        repository = new DecanoIndicadoresJpaRepository(entityManager);
        when(entityManager.createNativeQuery(anyString(), any(Class.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void consultar_kpis_ejecuta_el_sp_y_mapea_cada_cifra_sin_transformarla() {
        final SpKpisDecanoEntity fila = mock(SpKpisDecanoEntity.class);
        when(fila.idFacultad()).thenReturn(FACULTAD);
        when(fila.nombreFacultad()).thenReturn("Ingenieria");
        when(fila.idPeriodoAcademico()).thenReturn(PERIODO);
        when(fila.nombrePeriodoAcademico()).thenReturn("2026-2");
        when(fila.totalProgramas()).thenReturn(3);
        when(fila.totalEstudiantes()).thenReturn(6);
        when(fila.estudiantesRegulares()).thenReturn(2);
        when(fila.estudiantesRiesgo()).thenReturn(1);
        when(fila.estudiantesPerdida()).thenReturn(3);
        when(fila.tasaRetencion()).thenReturn(new BigDecimal("50.00"));
        when(fila.sesionesEjecutadas()).thenReturn(35);
        when(fila.faltasSinJustaCausa()).thenReturn(6);
        when(fila.porcentajeAusentismo()).thenReturn(new BigDecimal("17.14"));
        when(fila.porcentajeAsistencia()).thenReturn(new BigDecimal("82.86"));
        when(fila.gruposActivos()).thenReturn(3);
        when(fila.cupoTotal()).thenReturn(55);
        when(fila.matriculadosActivos()).thenReturn(7);
        when(fila.tasaAforo()).thenReturn(new BigDecimal("12.73"));
        when(fila.umbralAdvertencia()).thenReturn(new BigDecimal("15.00"));
        when(fila.umbralPerdida()).thenReturn(new BigDecimal("20.00"));
        when(query.getResultList()).thenReturn(List.of(fila));

        final KpisDecanoProjection k = repository.consultarKpis(ACTOR, PERIODO);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_kpis_decano"), eq(SpKpisDecanoEntity.class));
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idPeriodoAcademico", PERIODO);
        assertEquals(FACULTAD, k.idFacultad());
        assertEquals(3, k.totalProgramas());
        assertEquals(6, k.totalEstudiantes());
        assertEquals(new BigDecimal("50.00"), k.tasaRetencion());
        assertEquals(35, k.sesionesEjecutadas());
        assertEquals(new BigDecimal("17.14"), k.porcentajeAusentismo());
        assertEquals(new BigDecimal("82.86"), k.porcentajeAsistencia());
        assertEquals(55, k.cupoTotal());
        assertEquals(new BigDecimal("12.73"), k.tasaAforo());
        assertEquals(new BigDecimal("20.00"), k.umbralPerdida());
    }

    @Test
    void consultar_kpis_conserva_los_nulos_y_trata_los_contadores_nulos_como_cero() {
        final SpKpisDecanoEntity fila = mock(SpKpisDecanoEntity.class);
        when(fila.idFacultad()).thenReturn(FACULTAD);
        when(query.getResultList()).thenReturn(List.of(fila));

        final KpisDecanoProjection k = repository.consultarKpis(ACTOR, null);

        assertNull(k.idPeriodoAcademico());
        assertNull(k.tasaRetencion());
        assertNull(k.porcentajeAusentismo());
        assertNull(k.porcentajeAsistencia());
        assertNull(k.tasaAforo());
        assertEquals(0, k.totalEstudiantes());
        assertEquals(0, k.gruposActivos());
        verify(query).setParameter("idPeriodoAcademico", null);
    }

    @Test
    void consultar_kpis_sin_fila_es_un_fallo_tecnico() {
        when(query.getResultList()).thenReturn(List.of());

        assertThrows(DatabaseOperationException.class, () -> repository.consultarKpis(ACTOR, null));
    }

    @Test
    void consultar_comparativa_mapea_cada_fila_y_conserva_nulos() {
        final SpComparativaProgramaEntity p1 = mock(SpComparativaProgramaEntity.class);
        when(p1.idPrograma()).thenReturn(PROGRAMA);
        when(p1.nombrePrograma()).thenReturn("Programa 1");
        when(p1.estaActivoPrograma()).thenReturn(true);
        when(p1.totalEstudiantes()).thenReturn(5);
        when(p1.estudiantesRiesgo()).thenReturn(1);
        when(p1.estudiantesPerdida()).thenReturn(2);
        when(p1.tasaRetencion()).thenReturn(new BigDecimal("60.00"));
        when(p1.sesionesEjecutadas()).thenReturn(33);
        when(p1.faltasSinJustaCausa()).thenReturn(5);
        when(p1.porcentajeAusentismo()).thenReturn(new BigDecimal("15.15"));
        final SpComparativaProgramaEntity p4 = mock(SpComparativaProgramaEntity.class);
        when(p4.idPrograma()).thenReturn(UUID.randomUUID());
        when(p4.nombrePrograma()).thenReturn("Programa 4");
        when(p4.estaActivoPrograma()).thenReturn(false);
        when(query.getResultList()).thenReturn(List.of(p1, p4));

        final List<ComparativaProgramaProjection> r = repository.consultarComparativa(ACTOR, PERIODO);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_comparativa_programas_facultad"), eq(SpComparativaProgramaEntity.class));
        verify(query).setParameter("idPeriodoAcademico", PERIODO);
        assertEquals(2, r.size());
        assertEquals(new BigDecimal("15.15"), r.get(0).porcentajeAusentismo());
        assertEquals(33, r.get(0).sesionesEjecutadas());
        assertTrue(r.get(0).estaActivoPrograma());
        assertNull(r.get(1).tasaRetencion());
        assertNull(r.get(1).porcentajeAusentismo());
        assertEquals(0, r.get(1).totalEstudiantes());
        assertEquals(false, r.get(1).estaActivoPrograma());
    }

    @Test
    void consultar_aforo_envia_filtros_y_mapea_filas_y_total() {
        final SpAforoGrupoEntity g1 = mock(SpAforoGrupoEntity.class);
        when(g1.idGrupo()).thenReturn(GRUPO);
        when(g1.codigoGrupo()).thenReturn(931001);
        when(g1.idAsignatura()).thenReturn(ASIGNATURA);
        when(g1.nombreAsignatura()).thenReturn("Asignatura Uno");
        when(g1.idPrograma()).thenReturn(PROGRAMA);
        when(g1.nombrePrograma()).thenReturn("Programa 1");
        when(g1.cupoMaximo()).thenReturn(10);
        when(g1.matriculadosActivos()).thenReturn(5);
        when(g1.cuposDisponibles()).thenReturn(5);
        when(g1.tasaAforo()).thenReturn(new BigDecimal("50.00"));
        when(g1.grupoEstaHabilitado()).thenReturn(true);
        when(g1.totalRegistros()).thenReturn(12);
        final SpAforoGrupoEntity g2 = mock(SpAforoGrupoEntity.class);
        when(g2.idGrupo()).thenReturn(UUID.randomUUID());
        when(g2.grupoEstaHabilitado()).thenReturn(null);
        when(query.getResultList()).thenReturn(List.of(g1, g2));

        final PaginaAforoGruposProjection r = repository.consultarAforo(ACTOR, PERIODO, PROGRAMA, false, 2, 5);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_aforo_grupos_facultad"), eq(SpAforoGrupoEntity.class));
        verify(query).setParameter("numeroPagina", 2);
        verify(query).setParameter("tamanoPagina", 5);
        verify(query).setParameter("idPrograma", PROGRAMA);
        verify(query).setParameter("soloHabilitados", false);
        verify(query).setParameter("idPeriodoAcademico", PERIODO);
        assertEquals(12, r.totalRegistros());
        assertEquals(2, r.grupos().size());
        assertEquals(10, r.grupos().get(0).cupoMaximo());
        assertEquals(new BigDecimal("50.00"), r.grupos().get(0).tasaAforo());
        assertTrue(r.grupos().get(0).grupoEstaHabilitado());
        assertNull(r.grupos().get(1).tasaAforo());
        assertEquals(false, r.grupos().get(1).grupoEstaHabilitado());
        assertEquals(0, r.grupos().get(1).cupoMaximo());
    }

    @Test
    void consultar_aforo_sin_filas_devuelve_total_cero() {
        when(query.getResultList()).thenReturn(List.of());

        final PaginaAforoGruposProjection r = repository.consultarAforo(ACTOR, null, null, true, 1, 10);

        assertEquals(0, r.totalRegistros());
        assertTrue(r.grupos().isEmpty());
    }

    @Test
    void traduce_los_codigos_de_db_a_errores_de_negocio_con_la_entidad_correcta() {
        assertThrows(ForbiddenException.class, () -> fallaCon("DBCODE=KPI_003|ambito"));
        final ResourceNotFoundException noEncontrado = assertThrows(ResourceNotFoundException.class, () -> fallaCon("DBCODE=GEN_001|Programa"));
        assertEquals("El programa especificado no existe en el sistema.", noEncontrado.getMessage());
        assertThrows(ResourceNotFoundException.class, () -> fallaCon("DBCODE=PER_001|periodo"));
        assertThrows(ValidationException.class, () -> fallaCon("DBCODE=PAG_001|paginacion"));
        assertThrows(ValidationException.class, () -> fallaCon("DBCODE=VAL_001|ejecutor"));
        assertThrows(DatabaseOperationException.class, () -> fallaCon("DBCODE=KPI_002|umbral"));
        assertThrows(DatabaseOperationException.class, () -> fallaCon("otro fallo"));
    }

    private void fallaCon(final String mensaje) {
        Mockito.doThrow(new PersistenceException("x", new RuntimeException(mensaje))).when(query).getResultList();
        repository.consultarAforo(ACTOR, null, null, true, 1, 10);
    }

    @Test
    void el_entity_manager_es_obligatorio() {
        assertThrows(NullPointerException.class, () -> new DecanoIndicadoresJpaRepository(null));
    }
}
