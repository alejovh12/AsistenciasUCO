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
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ComparativaFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.KpisInstitucionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PuntoTendenciaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpComparativaFacultadEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpKpisInstitucionEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpPuntoTendenciaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** HU133-HU139: adapter JPA de KPIs del administrador (EXEC nativo mapeado fielmente, sin recalcular). */
class AdminIndicadoresJpaRepositoryTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID INSTITUCION = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PERIODO = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID FACULTAD = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private AdminIndicadoresJpaRepository repository;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        repository = new AdminIndicadoresJpaRepository(entityManager);
        when(entityManager.createNativeQuery(anyString(), any(Class.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void consultar_kpis_ejecuta_el_sp_y_mapea_cada_cifra_sin_transformarla() {
        final SpKpisInstitucionEntity f = mock(SpKpisInstitucionEntity.class);
        when(f.idInstitucion()).thenReturn(INSTITUCION);
        when(f.nombreInstitucion()).thenReturn("Universidad");
        when(f.idPeriodoAcademico()).thenReturn(PERIODO);
        when(f.nombrePeriodoAcademico()).thenReturn("2026-2");
        when(f.totalFacultades()).thenReturn(3);
        when(f.totalProgramas()).thenReturn(4);
        when(f.totalEstudiantes()).thenReturn(6);
        when(f.estudiantesRegulares()).thenReturn(2);
        when(f.estudiantesRiesgo()).thenReturn(1);
        when(f.estudiantesPerdida()).thenReturn(3);
        when(f.tasaRetencion()).thenReturn(new BigDecimal("50.00"));
        when(f.sesionesEjecutadas()).thenReturn(35);
        when(f.faltasSinJustaCausa()).thenReturn(6);
        when(f.porcentajeAusentismo()).thenReturn(new BigDecimal("17.14"));
        when(f.porcentajeAsistencia()).thenReturn(new BigDecimal("82.86"));
        when(f.sesionesVencidas()).thenReturn(12);
        when(f.sesionesConRegistro()).thenReturn(11);
        when(f.porcentajeCumplimiento()).thenReturn(new BigDecimal("91.67"));
        when(f.estudiantesConMatriculaActiva()).thenReturn(6);
        when(f.estudiantesRegistrados()).thenReturn(7);
        when(f.docentesConGrupoActivo()).thenReturn(1);
        when(f.docentesRegistrados()).thenReturn(2);
        when(f.umbralAdvertencia()).thenReturn(new BigDecimal("15.00"));
        when(f.umbralPerdida()).thenReturn(new BigDecimal("20.00"));
        when(query.getResultList()).thenReturn(List.of(f));

        final KpisInstitucionProjection k = repository.consultarKpis(ACTOR, PERIODO);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_kpis_institucion"), eq(SpKpisInstitucionEntity.class));
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idPeriodoAcademico", PERIODO);
        assertEquals(INSTITUCION, k.idInstitucion());
        assertEquals("Universidad", k.nombreInstitucion());
        assertEquals(PERIODO, k.idPeriodoAcademico());
        assertEquals("2026-2", k.nombrePeriodoAcademico());
        assertEquals(3, k.totalFacultades());
        assertEquals(4, k.totalProgramas());
        assertEquals(6, k.totalEstudiantes());
        assertEquals(2, k.estudiantesRegulares());
        assertEquals(1, k.estudiantesRiesgo());
        assertEquals(3, k.estudiantesPerdida());
        assertEquals(new BigDecimal("50.00"), k.tasaRetencion());
        assertEquals(35, k.sesionesEjecutadas());
        assertEquals(6, k.faltasSinJustaCausa());
        assertEquals(new BigDecimal("17.14"), k.porcentajeAusentismo());
        assertEquals(new BigDecimal("82.86"), k.porcentajeAsistencia());
        assertEquals(12, k.sesionesVencidas());
        assertEquals(11, k.sesionesConRegistro());
        assertEquals(new BigDecimal("91.67"), k.porcentajeCumplimiento());
        assertEquals(6, k.estudiantesConMatriculaActiva());
        assertEquals(7, k.estudiantesRegistrados());
        assertEquals(1, k.docentesConGrupoActivo());
        assertEquals(2, k.docentesRegistrados());
        assertEquals(new BigDecimal("15.00"), k.umbralAdvertencia());
        assertEquals(new BigDecimal("20.00"), k.umbralPerdida());
    }

    @Test
    void consultar_kpis_conserva_los_nulos_y_trata_los_contadores_nulos_como_cero() {
        final SpKpisInstitucionEntity f = mock(SpKpisInstitucionEntity.class);
        when(f.idInstitucion()).thenReturn(INSTITUCION);
        when(query.getResultList()).thenReturn(List.of(f));

        final KpisInstitucionProjection k = repository.consultarKpis(ACTOR, null);

        assertNull(k.idPeriodoAcademico());
        assertNull(k.tasaRetencion());
        assertNull(k.porcentajeAusentismo());
        assertNull(k.porcentajeAsistencia());
        assertNull(k.porcentajeCumplimiento());
        assertEquals(0, k.totalEstudiantes());
        assertEquals(0, k.sesionesVencidas());
        assertEquals(0, k.docentesRegistrados());
        verify(query).setParameter("idPeriodoAcademico", null);
    }

    @Test
    void consultar_kpis_sin_fila_es_un_fallo_tecnico() {
        when(query.getResultList()).thenReturn(List.of());

        assertThrows(DatabaseOperationException.class, () -> repository.consultarKpis(ACTOR, null));
    }

    @Test
    void consultar_comparativa_mapea_cada_fila_y_conserva_nulos() {
        final SpComparativaFacultadEntity f1 = mock(SpComparativaFacultadEntity.class);
        when(f1.idFacultad()).thenReturn(FACULTAD);
        when(f1.nombreFacultad()).thenReturn("Ingenieria");
        when(f1.estaActivaFacultad()).thenReturn(true);
        when(f1.totalProgramas()).thenReturn(3);
        when(f1.totalEstudiantes()).thenReturn(6);
        when(f1.estudiantesRiesgo()).thenReturn(1);
        when(f1.estudiantesPerdida()).thenReturn(3);
        when(f1.tasaRetencion()).thenReturn(new BigDecimal("50.00"));
        when(f1.sesionesEjecutadas()).thenReturn(35);
        when(f1.faltasSinJustaCausa()).thenReturn(6);
        when(f1.porcentajeAusentismo()).thenReturn(new BigDecimal("17.14"));
        final SpComparativaFacultadEntity f3 = mock(SpComparativaFacultadEntity.class);
        when(f3.idFacultad()).thenReturn(UUID.randomUUID());
        when(f3.nombreFacultad()).thenReturn("Cerrada");
        when(query.getResultList()).thenReturn(List.of(f1, f3));

        final List<ComparativaFacultadProjection> r = repository.consultarComparativa(ACTOR, PERIODO);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_comparativa_facultades"), eq(SpComparativaFacultadEntity.class));
        verify(query).setParameter("idPeriodoAcademico", PERIODO);
        assertEquals(2, r.size());
        assertEquals(new BigDecimal("17.14"), r.get(0).porcentajeAusentismo());
        assertEquals(35, r.get(0).sesionesEjecutadas());
        assertTrue(r.get(0).estaActivaFacultad());
        assertNull(r.get(1).tasaRetencion());
        assertNull(r.get(1).porcentajeAusentismo());
        assertEquals(0, r.get(1).totalEstudiantes());
        assertEquals(false, r.get(1).estaActivaFacultad());
    }

    @Test
    void consultar_tendencia_envia_la_granularidad_y_mapea_los_puntos() {
        final SpPuntoTendenciaEntity p1 = mock(SpPuntoTendenciaEntity.class);
        when(p1.inicioIntervalo()).thenReturn(LocalDate.of(2026, 3, 2));
        when(p1.finIntervalo()).thenReturn(LocalDate.of(2026, 3, 8));
        when(p1.sesionesEjecutadas()).thenReturn(20);
        when(p1.faltasSinJustaCausa()).thenReturn(3);
        when(p1.porcentajeAusentismo()).thenReturn(new BigDecimal("15.00"));
        final SpPuntoTendenciaEntity p2 = mock(SpPuntoTendenciaEntity.class);
        when(p2.inicioIntervalo()).thenReturn(LocalDate.of(2026, 3, 9));
        when(query.getResultList()).thenReturn(List.of(p1, p2));

        final List<PuntoTendenciaProjection> r = repository.consultarTendencia(ACTOR, PERIODO, "MES");

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_tendencia_ausentismo"), eq(SpPuntoTendenciaEntity.class));
        verify(query).setParameter("granularidad", "MES");
        verify(query).setParameter("idPeriodoAcademico", PERIODO);
        assertEquals(2, r.size());
        assertEquals(LocalDate.of(2026, 3, 2), r.get(0).inicioIntervalo());
        assertEquals(LocalDate.of(2026, 3, 8), r.get(0).finIntervalo());
        assertEquals(20, r.get(0).sesionesEjecutadas());
        assertEquals(3, r.get(0).faltasSinJustaCausa());
        assertEquals(new BigDecimal("15.00"), r.get(0).porcentajeAusentismo());
        assertNull(r.get(1).porcentajeAusentismo());
        assertEquals(0, r.get(1).sesionesEjecutadas());
    }

    @Test
    void traduce_los_codigos_de_db_a_errores_de_negocio() {
        assertThrows(ForbiddenException.class, () -> fallaCon("DBCODE=KPI_004|ambito"));
        assertThrows(ResourceNotFoundException.class, () -> fallaCon("DBCODE=PER_001|periodo"));
        assertThrows(ValidationException.class, () -> fallaCon("DBCODE=GEN_002|granularidad"));
        assertThrows(ValidationException.class, () -> fallaCon("DBCODE=VAL_001|ejecutor"));
        assertThrows(DatabaseOperationException.class, () -> fallaCon("DBCODE=KPI_002|umbral"));
        assertThrows(DatabaseOperationException.class, () -> fallaCon("otro fallo"));
    }

    private void fallaCon(final String mensaje) {
        Mockito.doThrow(new PersistenceException("x", new RuntimeException(mensaje))).when(query).getResultList();
        repository.consultarTendencia(ACTOR, null, "SEMANA");
    }

    @Test
    void el_entity_manager_es_obligatorio() {
        assertThrows(NullPointerException.class, () -> new AdminIndicadoresJpaRepository(null));
    }
}
