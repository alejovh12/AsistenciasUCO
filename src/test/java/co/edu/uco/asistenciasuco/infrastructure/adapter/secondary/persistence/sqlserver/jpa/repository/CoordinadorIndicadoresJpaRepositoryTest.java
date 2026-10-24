package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AlertaTempranaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.BitacoraSesionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.FichaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.KpisCoordinadorProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaAlertasProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaBitacoraProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResumenAsignaturaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpAlertaTempranaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpBitacoraSesionEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpFichaEstudianteEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpKpisCoordinadorEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpResumenAsignaturaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

/** HU065-HU070: adapter JPA de KPIs del coordinador (EXEC nativo mapeado fielmente, sin recalcular). */
class CoordinadorIndicadoresJpaRepositoryTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PROGRAMA = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PERIODO = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ESTUDIANTE = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID GRUPO = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID ASIGNATURA = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private CoordinadorIndicadoresJpaRepository repository;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        repository = new CoordinadorIndicadoresJpaRepository(entityManager);
        when(entityManager.createNativeQuery(anyString(), any(Class.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void consultar_kpis_ejecuta_el_sp_y_mapea_cada_cifra_sin_transformarla() {
        final SpKpisCoordinadorEntity fila = mock(SpKpisCoordinadorEntity.class);
        when(fila.idPrograma()).thenReturn(PROGRAMA);
        when(fila.nombrePrograma()).thenReturn("Sistemas");
        when(fila.idPeriodoAcademico()).thenReturn(PERIODO);
        when(fila.nombrePeriodoAcademico()).thenReturn("2026-2");
        when(fila.totalEstudiantes()).thenReturn(5);
        when(fila.estudiantesRegulares()).thenReturn(2);
        when(fila.estudiantesRiesgo()).thenReturn(1);
        when(fila.estudiantesPerdida()).thenReturn(2);
        when(fila.tasaPermanencia()).thenReturn(new BigDecimal("60.00"));
        when(fila.sesionesProgramadas()).thenReturn(15);
        when(fila.sesionesVencidas()).thenReturn(10);
        when(fila.sesionesEjecutadas()).thenReturn(9);
        when(fila.sesionesVencidasSinRegistro()).thenReturn(1);
        when(fila.sesionesSinProcedenciaTemporal()).thenReturn(1);
        when(fila.porcentajeCumplimiento()).thenReturn(new BigDecimal("90.00"));
        when(fila.porcentajeProgresoCalendario()).thenReturn(new BigDecimal("66.67"));
        when(fila.umbralAdvertencia()).thenReturn(new BigDecimal("15.00"));
        when(fila.umbralPerdida()).thenReturn(new BigDecimal("20.00"));
        when(query.getResultList()).thenReturn(List.of(fila));

        final KpisCoordinadorProjection kpis = repository.consultarKpis(ACTOR, PERIODO);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_kpis_coordinador"), eq(SpKpisCoordinadorEntity.class));
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idPeriodoAcademico", PERIODO);
        assertEquals(5, kpis.totalEstudiantes());
        assertEquals(new BigDecimal("60.00"), kpis.tasaPermanencia());
        assertEquals(9, kpis.sesionesEjecutadas());
        assertEquals(new BigDecimal("90.00"), kpis.porcentajeCumplimiento());
        assertEquals(new BigDecimal("20.00"), kpis.umbralPerdida());
    }

    @Test
    void consultar_kpis_conserva_los_nulos_y_trata_los_contadores_nulos_como_cero() {
        final SpKpisCoordinadorEntity fila = mock(SpKpisCoordinadorEntity.class);
        when(fila.idPrograma()).thenReturn(PROGRAMA);
        when(query.getResultList()).thenReturn(List.of(fila));

        final KpisCoordinadorProjection kpis = repository.consultarKpis(ACTOR, null);

        assertNull(kpis.tasaPermanencia());
        assertNull(kpis.porcentajeCumplimiento());
        assertNull(kpis.idPeriodoAcademico());
        assertEquals(0, kpis.totalEstudiantes());
        assertEquals(0, kpis.sesionesProgramadas());
    }

    @Test
    void consultar_kpis_sin_filas_es_un_error_tecnico() {
        when(query.getResultList()).thenReturn(List.of());

        assertThrows(DatabaseOperationException.class, () -> repository.consultarKpis(ACTOR, null));
    }

    @Test
    void consultar_alertas_ejecuta_el_sp_paginado_y_mapea_filas_y_total() {
        final SpAlertaTempranaEntity fila = mock(SpAlertaTempranaEntity.class);
        when(fila.idEstudiante()).thenReturn(ESTUDIANTE);
        when(fila.nombreCompleto()).thenReturn("Ana Perez");
        when(fila.numeroIdentificacion()).thenReturn(1017000001);
        when(fila.correo()).thenReturn("ana@uco.edu.co");
        when(fila.totalMatriculasActivas()).thenReturn(2);
        when(fila.porcentajeInasistencia()).thenReturn(new BigDecimal("66.67"));
        when(fila.faltasSinJustaCausa()).thenReturn(2);
        when(fila.codigoPermanencia()).thenReturn("PERDIDA");
        when(fila.semaforo()).thenReturn("ROJO");
        when(fila.totalRegistros()).thenReturn(12);
        when(query.getResultList()).thenReturn(List.of(fila));

        final PaginaAlertasProjection pagina = repository.consultarAlertas(ACTOR, PERIODO, "ROJO", "Ana", 2, 5);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_alerta_temprana_programa"), eq(SpAlertaTempranaEntity.class));
        verify(query).setParameter("numeroPagina", 2);
        verify(query).setParameter("tamanoPagina", 5);
        verify(query).setParameter("semaforo", "ROJO");
        verify(query).setParameter("filtroTexto", "Ana");
        verify(query).setParameter("idPeriodoAcademico", PERIODO);
        assertEquals(12, pagina.totalRegistros());
        final AlertaTempranaProjection alerta = pagina.alertas().getFirst();
        assertEquals(ESTUDIANTE, alerta.idEstudiante());
        assertEquals(new BigDecimal("66.67"), alerta.porcentajeInasistencia());
        assertEquals("ROJO", alerta.semaforo());
        assertEquals(2, alerta.totalMatriculasActivas());
    }

    @Test
    void consultar_alertas_sin_filas_devuelve_pagina_vacia_con_total_cero() {
        when(query.getResultList()).thenReturn(List.of());

        final PaginaAlertasProjection pagina = repository.consultarAlertas(ACTOR, null, null, null, 1, 10);

        assertTrue(pagina.alertas().isEmpty());
        assertEquals(0, pagina.totalRegistros());
    }

    @Test
    void consultar_bitacora_convierte_las_horas_a_utc_y_conserva_asistio_nulo() {
        final SpBitacoraSesionEntity fila = mock(SpBitacoraSesionEntity.class);
        when(fila.idSesion()).thenReturn(UUID.randomUUID());
        when(fila.numeroSesion()).thenReturn(7);
        when(fila.nombreSesion()).thenReturn("Sesion G1-7");
        when(fila.fechaHoraInicio()).thenReturn(LocalDateTime.of(2026, 10, 23, 14, 0));
        when(fila.fechaHoraFin()).thenReturn(LocalDateTime.of(2026, 10, 23, 16, 0));
        when(fila.idGrupo()).thenReturn(GRUPO);
        when(fila.codigoGrupo()).thenReturn(931001);
        when(fila.idAsignatura()).thenReturn(ASIGNATURA);
        when(fila.nombreAsignatura()).thenReturn("Estructuras");
        when(fila.totalRegistros()).thenReturn(25);
        when(query.getResultList()).thenReturn(List.of(fila));

        final PaginaBitacoraProjection pagina = repository.consultarBitacora(ACTOR, ESTUDIANTE, PERIODO, 3, 10);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_bitacora_estudiante_programa"), eq(SpBitacoraSesionEntity.class));
        verify(query).setParameter("idEstudiante", ESTUDIANTE);
        assertEquals(25, pagina.totalRegistros());
        final BitacoraSesionProjection sesion = pagina.sesiones().getFirst();
        assertEquals(Instant.parse("2026-10-23T14:00:00Z"), sesion.fechaHoraInicio());
        assertEquals(Instant.parse("2026-10-23T16:00:00Z"), sesion.fechaHoraFin());
        assertNull(sesion.asistio());
        assertEquals(931001, sesion.codigoGrupo());
    }

    @Test
    void consultar_ficha_mapea_la_cabecera_sin_telefono_y_falla_si_no_hay_fila() {
        final SpFichaEstudianteEntity fila = mock(SpFichaEstudianteEntity.class);
        when(fila.idEstudiante()).thenReturn(ESTUDIANTE);
        when(fila.tipoIdentificacion()).thenReturn("CC");
        when(fila.numeroIdentificacion()).thenReturn(1017000001);
        when(fila.nombreCompleto()).thenReturn("Ana Perez");
        when(fila.correo()).thenReturn("ana@uco.edu.co");
        when(fila.estaActivoUsuario()).thenReturn(Boolean.TRUE);
        when(fila.idPrograma()).thenReturn(PROGRAMA);
        when(fila.nombrePrograma()).thenReturn("Sistemas");
        when(query.getResultList()).thenReturn(List.of(fila));

        final FichaEstudianteProjection ficha = repository.consultarFicha(ACTOR, ESTUDIANTE);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_ficha_estudiante_programa"), eq(SpFichaEstudianteEntity.class));
        assertEquals("ana@uco.edu.co", ficha.correo());
        assertTrue(ficha.estaActivoUsuario());

        when(query.getResultList()).thenReturn(List.of());
        assertThrows(DatabaseOperationException.class, () -> repository.consultarFicha(ACTOR, ESTUDIANTE));
    }

    @Test
    void consultar_resumen_mapea_una_fila_por_matricula() {
        final SpResumenAsignaturaEntity fila = mock(SpResumenAsignaturaEntity.class);
        when(fila.idGrupo()).thenReturn(GRUPO);
        when(fila.codigoGrupo()).thenReturn(931001);
        when(fila.idAsignatura()).thenReturn(ASIGNATURA);
        when(fila.nombreAsignatura()).thenReturn("Estructuras");
        when(fila.codigoEstadoEstudiante()).thenReturn("A");
        when(fila.nombreEstadoEstudiante()).thenReturn("activo");
        when(fila.sesionesEjecutadas()).thenReturn(6);
        when(fila.asistencias()).thenReturn(4);
        when(fila.faltasSinJustaCausa()).thenReturn(2);
        when(fila.excusas()).thenReturn(0);
        when(fila.porcentajeInasistencia()).thenReturn(new BigDecimal("33.33"));
        when(fila.codigoPermanencia()).thenReturn("PERDIDA");
        when(query.getResultList()).thenReturn(List.of(fila));

        final List<ResumenAsignaturaProjection> resumen = repository.consultarResumenAsistencia(ACTOR, ESTUDIANTE, null);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_resumen_asistencia_estudiante_programa"), eq(SpResumenAsignaturaEntity.class));
        assertEquals(1, resumen.size());
        assertEquals(new BigDecimal("33.33"), resumen.getFirst().porcentajeInasistencia());
        assertEquals("PERDIDA", resumen.getFirst().codigoPermanencia());
    }

    @Test
    void traduce_los_codigos_de_db_a_errores_de_negocio_y_deja_los_demas_como_tecnicos() {
        assertThrows(ForbiddenException.class, () -> fallaCon("DBCODE=KPI_001|ambito"));
        assertThrows(ResourceNotFoundException.class, () -> fallaCon("DBCODE=GEN_001|Estudiante"));
        assertThrows(ResourceNotFoundException.class, () -> fallaCon("DBCODE=PER_001|periodo"));
        assertThrows(ValidationException.class, () -> fallaCon("DBCODE=PAG_001|paginacion"));
        assertThrows(ValidationException.class, () -> fallaCon("DBCODE=GEN_002|semaforo"));
        assertThrows(ValidationException.class, () -> fallaCon("DBCODE=VAL_001|ejecutor"));
        assertThrows(DatabaseOperationException.class, () -> fallaCon("DBCODE=KPI_002|umbral"));
        assertThrows(DatabaseOperationException.class, () -> fallaCon("otro fallo"));
    }

    private void fallaCon(final String mensaje) {
        Mockito.doThrow(new PersistenceException("x", new RuntimeException(mensaje))).when(query).getResultList();
        repository.consultarAlertas(ACTOR, null, null, null, 1, 10);
    }

    @Test
    void el_entity_manager_es_obligatorio() {
        assertThrows(NullPointerException.class, () -> new CoordinadorIndicadoresJpaRepository(null));
    }
}
