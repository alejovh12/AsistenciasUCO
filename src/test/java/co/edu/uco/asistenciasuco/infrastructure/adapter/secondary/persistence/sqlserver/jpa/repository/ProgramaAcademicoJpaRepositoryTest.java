package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaAcademicoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaAcademicoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ProgramaAcademicoPaginaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ProgramaAcademicoProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpProgramaAcademicoPaginadoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvProgramaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU076-HU078 / HU108: adapter JPA del programa academico (comandos por EXEC y consulta paginada). */
class ProgramaAcademicoJpaRepositoryTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FACULTAD = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PROGRAMA = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TIPO = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID COORDINADOR = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID MODALIDAD = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID JORNADA = UUID.fromString("77777777-7777-7777-7777-777777777777");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private ProgramaAcademicoJpaRepository repository;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        repository = new ProgramaAcademicoJpaRepository(entityManager, new JpaProcedureExecutor(entityManager));
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(entityManager.createNativeQuery(anyString(), any(Class.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{CORRELATION, "ok", "", true}));
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void crear_ejecuta_el_sp_con_binding_nombrado_y_la_correlacion() {
        repository.crear(new ProgramaAcademicoCommandPort.CrearProgramaCommand(
                PROGRAMA, FACULTAD, "Sistemas", TIPO, COORDINADOR, MODALIDAD, JORNADA, ACTOR));

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_crear_programa_academico"));
        verify(query).setParameter("idPrograma", PROGRAMA);
        verify(query).setParameter("idFacultad", FACULTAD);
        verify(query).setParameter("nombre", "Sistemas");
        verify(query).setParameter("idTipoPrograma", TIPO);
        verify(query).setParameter("idCoordinador", COORDINADOR);
        verify(query).setParameter("idModalidad", MODALIDAD);
        verify(query).setParameter("idJornada", JORNADA);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
        verify(entityManager, never()).createStoredProcedureQuery(anyString());
    }

    @Test
    void actualizar_ejecuta_el_sp_de_edicion() {
        repository.actualizar(new ProgramaAcademicoCommandPort.ActualizarProgramaCommand(
                PROGRAMA, "Sistemas", TIPO, COORDINADOR, null, null, ACTOR));

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_actualizar_programa_academico"));
        verify(query).setParameter("idPrograma", PROGRAMA);
        verify(query).setParameter("nombre", "Sistemas");
        verify(query).setParameter("idTipoPrograma", TIPO);
        verify(query).setParameter("idCoordinador", COORDINADOR);
        verify(query).setParameter("idModalidad", null);
        verify(query).setParameter("idJornada", null);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
    }

    @Test
    void cambiar_estado_ejecuta_el_sp_con_el_estado_destino() {
        repository.cambiarEstado(new ProgramaAcademicoCommandPort.CambiarEstadoProgramaCommand(PROGRAMA, false, ACTOR));

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_cambiar_estado_programa_academico"));
        verify(query).setParameter("idPrograma", PROGRAMA);
        verify(query).setParameter("estaActivo", false);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
    }

    @Test
    void consultar_ejecuta_el_sp_paginado_y_mapea_filas_y_total() {
        final SpProgramaAcademicoPaginadoEntity fila = mock(SpProgramaAcademicoPaginadoEntity.class);
        when(fila.id()).thenReturn(PROGRAMA);
        when(fila.nombrePrograma()).thenReturn("Sistemas");
        when(fila.idTipoPrograma()).thenReturn(TIPO);
        when(fila.nombreTipoPrograma()).thenReturn("pregrado");
        when(fila.idModalidad()).thenReturn(MODALIDAD);
        when(fila.nombreModalidad()).thenReturn("Presencial");
        when(fila.idJornada()).thenReturn(JORNADA);
        when(fila.nombreJornada()).thenReturn("Diurna");
        when(fila.idCoordinador()).thenReturn(COORDINADOR);
        when(fila.nombreCoordinador()).thenReturn("Ana Perez");
        when(fila.idFacultad()).thenReturn(FACULTAD);
        when(fila.nombreFacultad()).thenReturn("Ingenieria");
        when(fila.estaActivoPrograma()).thenReturn(true);
        when(fila.totalRegistros()).thenReturn(12);
        when(query.getResultList()).thenReturn(List.of(fila));

        final ProgramaAcademicoPaginaProjection pagina = repository.consultar(
                new ProgramaAcademicoQueryPort.ConsultaProgramas(ACTOR, "Sis", true, TIPO, 2, 5));

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_programas_academicos"), eq(SpProgramaAcademicoPaginadoEntity.class));
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("numeroPagina", 2);
        verify(query).setParameter("tamanoPagina", 5);
        verify(query).setParameter("nombre", "Sis");
        verify(query).setParameter("estaActivo", true);
        verify(query).setParameter("idTipoPrograma", TIPO);
        assertEquals(12, pagina.totalRegistros());
        final ProgramaAcademicoProjection programa = pagina.programas().getFirst();
        assertEquals(PROGRAMA, programa.id());
        assertEquals("Sistemas", programa.nombre());
        assertEquals("Presencial", programa.nombreModalidad());
        assertEquals("Ana Perez", programa.nombreCoordinador());
        assertTrue(programa.estaActivo());
    }

    @Test
    void consultar_sin_filas_devuelve_pagina_vacia_con_total_cero() {
        when(query.getResultList()).thenReturn(List.of());

        final ProgramaAcademicoPaginaProjection pagina = repository.consultar(
                new ProgramaAcademicoQueryPort.ConsultaProgramas(ACTOR, null, null, null, 1, 10));

        assertTrue(pagina.programas().isEmpty());
        assertEquals(0, pagina.totalRegistros());
    }

    @Test
    void consultar_trata_nulos_de_estado_y_total_de_forma_segura() {
        final SpProgramaAcademicoPaginadoEntity fila = mock(SpProgramaAcademicoPaginadoEntity.class);
        when(fila.id()).thenReturn(PROGRAMA);
        when(query.getResultList()).thenReturn(List.of(fila));

        final ProgramaAcademicoPaginaProjection pagina = repository.consultar(
                new ProgramaAcademicoQueryPort.ConsultaProgramas(ACTOR, null, null, null, 1, 10));

        assertFalse(pagina.programas().getFirst().estaActivo());
        assertEquals(0, pagina.totalRegistros());
        assertNull(pagina.programas().getFirst().idModalidad());
    }

    @Test
    @SuppressWarnings("unchecked")
    void consultar_por_id_filtra_por_facultad_y_mapea_la_vista() {
        final TypedQuery<UvProgramaEntity> typed = mock(TypedQuery.class);
        final UvProgramaEntity fila = mock(UvProgramaEntity.class);
        when(fila.id()).thenReturn(PROGRAMA);
        when(fila.nombrePrograma()).thenReturn("Sistemas");
        when(fila.estaActivoPrograma()).thenReturn(Boolean.TRUE);
        when(entityManager.createQuery(anyString(), eq(UvProgramaEntity.class))).thenReturn(typed);
        when(typed.setParameter(anyString(), any())).thenReturn(typed);
        when(typed.setMaxResults(1)).thenReturn(typed);
        when(typed.getResultList()).thenReturn(List.of(fila));

        final Optional<ProgramaAcademicoProjection> resultado = repository.consultarPorId(PROGRAMA, FACULTAD);

        verify(typed).setParameter("idPrograma", PROGRAMA);
        verify(typed).setParameter("idFacultad", FACULTAD);
        assertTrue(resultado.isPresent());
        assertEquals("Sistemas", resultado.get().nombre());
        assertTrue(resultado.get().estaActivo());
    }

    @Test
    @SuppressWarnings("unchecked")
    void consultar_por_id_inexistente_devuelve_vacio() {
        final TypedQuery<UvProgramaEntity> typed = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(UvProgramaEntity.class))).thenReturn(typed);
        when(typed.setParameter(anyString(), any())).thenReturn(typed);
        when(typed.setMaxResults(1)).thenReturn(typed);
        when(typed.getResultList()).thenReturn(List.of());

        assertTrue(repository.consultarPorId(PROGRAMA, FACULTAD).isEmpty());
    }

    @Test
    void T_108_04_pag_001_del_sp_se_traduce_a_validacion_y_otros_fallos_siguen_siendo_tecnicos() {
        when(query.getResultList()).thenThrow(new jakarta.persistence.PersistenceException(
                "x", new RuntimeException("DBCODE=PAG_001|Paginacion invalida")));
        final var consulta = new ProgramaAcademicoQueryPort.ConsultaProgramas(ACTOR, null, null, null, 1, 100000);
        assertThrows(co.edu.uco.asistenciasuco.application.exception.validation.ValidationException.class, () -> repository.consultar(consulta));

        org.mockito.Mockito.doThrow(new jakarta.persistence.PersistenceException("otro fallo")).when(query).getResultList();
        assertThrows(co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException.class,
                () -> repository.consultar(consulta));
    }

    @Test
    void el_entity_manager_es_obligatorio() {
        assertThrows(NullPointerException.class, () -> new ProgramaAcademicoJpaRepository(null, null));
    }
}
