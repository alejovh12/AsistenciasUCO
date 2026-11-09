package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaPlanProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CaracterAsignaturaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.GrupoFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaAsignaturasAsociablesProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaAsignaturasPlanProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaGruposFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaPlanesEstudioProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ProgramaGrupoProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CULM-001 B2NM: asignaturas por plan sobre SP (parametros enviados, mapeo por posicion de columna y traduccion de errores). */
class AsignaturaPlanEstudioJpaRepositoryTest {

    private static final UUID CORRELACION = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID PLAN = UUID.randomUUID();
    private static final UUID ASIGNATURA = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();

    private final EntityManager manager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private AsignaturaPlanEstudioJpaRepository repo;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELACION);
        when(manager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        repo = new AsignaturaPlanEstudioJpaRepository(manager, new JpaProcedureExecutor(manager));
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private static Object[] canonica(final boolean exitoso) {
        return new Object[]{CORRELACION.toString(), "mensaje", "DBCODE=OK|ok", exitoso};
    }

    private static Object[] filaAsignaturaPlan(final Object bool, final long total) {
        return new Object[]{ASIGNATURA.toString(), "MAT101", "Calculo", 3, UUID.randomUUID(), "Matematicas", UUID.randomUUID(), "Basico", PLAN.toString(), 2020,
                PROGRAMA.toString(), "Sistemas", UUID.randomUUID(), 1, "S1", "OBL", "Obligatoria", bool, 1, 0, total};
    }

    private static Object[] filaGrupo(final long total) {
        return new Object[]{GRUPO.toString(), 5, "G05", UUID.randomUUID(), ASIGNATURA, "Calculo", null, 30, 10, 20, 1, "Sistemas, Derecho , ", total};
    }

    @Test
    void asociar_y_desvincular_ejecutan_su_sp_con_los_parametros_y_la_correlacion() {
        when(query.getResultList()).thenReturn(Collections.<Object>singletonList(canonica(true)));

        repo.asociar(USUARIO, PLAN, ASIGNATURA, 3, "OBL");
        verify(manager).createNativeQuery(AsignaturaPlanEstudioJpaRepository.SQL_ASOCIAR);
        verify(query).setParameter("idAsignatura", ASIGNATURA);
        verify(query).setParameter("idPlanEstudio", PLAN);
        verify(query).setParameter("semestreNumero", 3);
        verify(query).setParameter("codigoCaracter", "OBL");
        verify(query).setParameter("idCorrelacion", CORRELACION);
        verify(query).setParameter("idUsuarioEjecutor", USUARIO);

        repo.desvincular(USUARIO, PLAN, ASIGNATURA);
        verify(manager).createNativeQuery(AsignaturaPlanEstudioJpaRepository.SQL_DESVINCULAR);
    }

    @Test
    void asociar_con_fallo_tecnico_de_la_db_es_database_operation_exception() {
        doThrow(new PersistenceException("caida")).when(query).getResultList();
        assertThrows(DatabaseOperationException.class, () -> repo.asociar(USUARIO, PLAN, ASIGNATURA, 1, "OBL"));
        assertThrows(DatabaseOperationException.class, () -> repo.desvincular(USUARIO, PLAN, ASIGNATURA));
    }

    @Test
    void consultar_asociables_mapea_las_filas_y_toma_el_total_de_la_ultima_columna() {
        when(query.getResultList()).thenReturn(filas(
                new Object[]{ASIGNATURA.toString(), "MAT101", "Calculo", 3, "Mat", "Basico", "P1", 7L},
                new Object[]{UUID.randomUUID(), "FIS101", "Fisica", null, null, null, null, 7L}));

        final PaginaAsignaturasAsociablesProjection pagina = repo.consultarAsociables(USUARIO, PLAN, "cal", 2, 10);

        assertEquals(7, pagina.total());
        assertEquals(2, pagina.elementos().size());
        assertEquals(ASIGNATURA, pagina.elementos().getFirst().idAsignatura());
        assertEquals("Calculo", pagina.elementos().getFirst().nombre());
        assertNull(pagina.elementos().get(1).credito());
        verify(manager).createNativeQuery(AsignaturaPlanEstudioJpaRepository.SQL_ASOCIABLES);
        verify(query).setParameter("idUsuarioEjecutor", USUARIO);
        verify(query).setParameter("idCorrelacion", CORRELACION);
        verify(query).setParameter("idPlanEstudio", PLAN);
        verify(query).setParameter("numeroPagina", 2);
        verify(query).setParameter("tamanoPagina", 10);
        verify(query).setParameter("busqueda", "cal");
    }

    @Test
    void consultar_asociables_sin_filas_devuelve_pagina_vacia() {
        when(query.getResultList()).thenReturn(List.of());
        final PaginaAsignaturasAsociablesProjection pagina = repo.consultarAsociables(USUARIO, PLAN, null, 1, 10);
        assertEquals(0, pagina.total());
        assertTrue(pagina.elementos().isEmpty());
    }

    @Test
    void consultar_caracteres_lee_la_vista_de_catalogo_y_mapea_tres_columnas() {
        when(query.getResultList()).thenReturn(filas(new Object[]{"OBL", "Obligatoria", 1}, new Object[]{"ELE", "Electiva", 2}));

        final List<CaracterAsignaturaProjection> caracteres = repo.consultarCaracteres();

        assertEquals(List.of(new CaracterAsignaturaProjection("OBL", "Obligatoria", 1), new CaracterAsignaturaProjection("ELE", "Electiva", 2)), caracteres);
        verify(manager).createNativeQuery(AsignaturaPlanEstudioJpaRepository.SQL_CARACTERES);
        verify(query, never()).setParameter(anyString(), any());
    }

    @Test
    void consultar_programas_grupo_envia_el_grupo_y_mapea_cuatro_columnas() {
        when(query.getResultList()).thenReturn(filas(new Object[]{PROGRAMA.toString(), "Sistemas", PLAN, 2020}));

        final List<ProgramaGrupoProjection> programas = repo.consultarProgramasGrupo(USUARIO, GRUPO);

        assertEquals(List.of(new ProgramaGrupoProjection(PROGRAMA, "Sistemas", PLAN, 2020)), programas);
        verify(query).setParameter("idGrupo", GRUPO);
    }

    @Test
    void consultar_malla_y_asignaturas_coordinador_mapean_21_columnas_con_booleanos_flexibles() {
        when(query.getResultList()).thenReturn(filas(filaAsignaturaPlan(Boolean.TRUE, 12L), filaAsignaturaPlan(0, 12L), filaAsignaturaPlan(null, 12L)));

        final PaginaAsignaturasPlanProjection malla = repo.consultarMalla(USUARIO, PLAN, 1, "OBL", "cal", 1, 20);

        assertEquals(12, malla.total());
        assertEquals(3, malla.elementos().size());
        final AsignaturaPlanProjection primera = malla.elementos().getFirst();
        assertEquals("MAT101", primera.codigo());
        assertEquals(ASIGNATURA, primera.idAsignatura());
        assertEquals(PROGRAMA, primera.idPrograma());
        assertTrue(primera.estaActivoVinculo());
        assertFalse(malla.elementos().get(1).estaActivoVinculo());
        assertFalse(malla.elementos().get(2).estaActivoVinculo());
        verify(manager).createNativeQuery(AsignaturaPlanEstudioJpaRepository.SQL_MALLA);
        verify(query).setParameter("numeroSemestre", 1);
        verify(query).setParameter("codigoCaracter", "OBL");

        final PaginaAsignaturasPlanProjection coordinador = repo.consultarAsignaturasCoordinador(USUARIO, PROGRAMA, "x", Boolean.TRUE, 2, 5);
        assertEquals(3, coordinador.elementos().size());
        verify(manager).createNativeQuery(AsignaturaPlanEstudioJpaRepository.SQL_ASIGNATURAS_COORDINADOR);
        verify(query).setParameter("idPrograma", PROGRAMA);
        verify(query).setParameter("estaActiva", Boolean.TRUE);
    }

    @Test
    void una_fila_con_columnas_distintas_a_las_del_contrato_es_fallo_tecnico() {
        when(query.getResultList()).thenReturn(Collections.<Object>singletonList(new Object[20]));
        final DatabaseOperationException error = assertThrows(DatabaseOperationException.class, () -> repo.consultarMalla(USUARIO, PLAN, null, null, null, 1, 20));
        assertTrue(error.getCause().getMessage().contains("21"));
    }

    @Test
    void consultar_planes_mapea_11_columnas() {
        when(query.getResultList()).thenReturn(filas(
                new Object[]{PLAN.toString(), PROGRAMA.toString(), "Sistemas", 2020, true, "Activo", "justif", 10, 50, 160, 3L},
                new Object[]{UUID.randomUUID(), PROGRAMA, "Sistemas", null, 0, "Inactivo", null, 8, 40, 120, 3L}));

        final PaginaPlanesEstudioProjection pagina = repo.consultarPlanes(USUARIO, PROGRAMA, null, 1, 10);

        assertEquals(3, pagina.total());
        assertEquals("2020", pagina.elementos().getFirst().inp());
        assertNull(pagina.elementos().get(1).inp());
        assertTrue(pagina.elementos().getFirst().estaActivoPlanEstudio());
        assertFalse(pagina.elementos().get(1).estaActivoPlanEstudio());
        assertEquals(160, pagina.elementos().getFirst().totalCreditos());
        verify(manager).createNativeQuery(AsignaturaPlanEstudioJpaRepository.SQL_PLANES);
        verify(query).setParameter("idPrograma", PROGRAMA);
    }

    @Test
    void consultar_grupos_facultad_separa_los_programas_por_coma() {
        when(query.getResultList()).thenReturn(filas(filaGrupo(4L)));
        final UUID periodo = UUID.randomUUID();

        final PaginaGruposFacultadProjection pagina = repo.consultarGruposFacultad(USUARIO, periodo, PROGRAMA, "cal", 1, 10);

        assertEquals(4, pagina.total());
        final GrupoFacultadProjection grupo = pagina.elementos().getFirst();
        assertEquals(GRUPO, grupo.idGrupo());
        assertEquals(List.of("Sistemas", "Derecho"), grupo.programas());
        assertTrue(grupo.grupoEstaHablitado());
        assertNull(grupo.idDocente());
        verify(manager).createNativeQuery(AsignaturaPlanEstudioJpaRepository.SQL_GRUPOS_FACULTAD);
        verify(query).setParameter("idPeriodoAcademico", periodo);
    }

    @Test
    void consultar_grupo_facultad_devuelve_el_detalle_con_programas_vacios_si_no_hay_texto() {
        final Object[] fila = filaGrupo(1L);
        fila[11] = " ";
        when(query.getResultList()).thenReturn(filas(java.util.Arrays.copyOf(fila, 12)));

        final GrupoFacultadProjection grupo = repo.consultarGrupoFacultad(USUARIO, GRUPO);

        assertTrue(grupo.programas().isEmpty());
        assertEquals(5, grupo.codigo());
        verify(manager).createNativeQuery(AsignaturaPlanEstudioJpaRepository.SQL_GRUPO_FACULTAD);
    }

    @Test
    void consultar_grupo_facultad_sin_fila_es_fallo_tecnico_y_con_columnas_erradas_tambien() {
        when(query.getResultList()).thenReturn(List.of());
        assertThrows(DatabaseOperationException.class, () -> repo.consultarGrupoFacultad(USUARIO, GRUPO));

        when(query.getResultList()).thenReturn(Collections.<Object>singletonList(new Object[13]));
        assertThrows(DatabaseOperationException.class, () -> repo.consultarGrupoFacultad(USUARIO, GRUPO));
    }

    @Test
    void los_rechazos_del_sp_en_consultas_se_traducen_y_los_tecnicos_no() {
        doThrow(new PersistenceException("x", new RuntimeException("DBCODE=SEC_001|perfil"))).when(query).getResultList();
        assertThrows(ForbiddenException.class, () -> repo.consultarPlanes(USUARIO, PROGRAMA, null, 1, 10));
        assertThrows(ForbiddenException.class, () -> repo.consultarGruposFacultad(USUARIO, null, null, null, 1, 10));

        doThrow(new PersistenceException("db down")).when(query).getResultList();
        assertThrows(DatabaseOperationException.class, () -> repo.consultarCaracteres());
    }

    @Test
    void los_argumentos_obligatorios_del_constructor_se_validan() {
        assertThrows(NullPointerException.class, () -> new AsignaturaPlanEstudioJpaRepository(null, new JpaProcedureExecutor(manager)));
        assertThrows(NullPointerException.class, () -> new AsignaturaPlanEstudioJpaRepository(manager, null));
    }

    /** Lista de filas nativas: evita que {@code List.of} expanda el arreglo de columnas como varargs. */
    private static List<Object> filas(final Object[]... filas) {
        return new java.util.ArrayList<>(java.util.Arrays.asList((Object[]) filas));
    }
}
