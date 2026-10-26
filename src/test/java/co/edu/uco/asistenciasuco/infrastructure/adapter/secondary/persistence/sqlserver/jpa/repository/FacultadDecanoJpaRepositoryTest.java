package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpEstructuraFacultadEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpEstudianteFacultadPaginadoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpMatriculaEstudianteFacultadEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpSesionFacultadPaginadoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU104/HU105/HU109/HU119/HU145: native EXEC de los SP de lectura del decano. */
class FacultadDecanoJpaRepositoryTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID EJECUTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PROGRAMA = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ESTUDIANTE = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private FacultadDecanoJpaRepository repository;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        repository = new FacultadDecanoJpaRepository(entityManager);
        when(entityManager.createNativeQuery(anyString(), any(Class.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void constructor_rechaza_entity_manager_nulo() {
        assertThrows(NullPointerException.class, () -> new FacultadDecanoJpaRepository(null));
    }

    @Test
    void T_104_PAT_01_estudiantes_enlaza_filtros_y_mapea_total() {
        when(query.getResultList()).thenReturn(List.of(
                entidad(SpEstudianteFacultadPaginadoEntity.class, Map.of("idEstudiante", ESTUDIANTE,
                        "numeroIdentificacion", 1017000111, "nombreCompleto", "Ana Ruiz", "estaActivoUsuario", true,
                        "programas", "Sistemas, Industrial", "totalGrupos", 3, "totalRegistros", 21)),
                entidad(SpEstudianteFacultadPaginadoEntity.class, Map.of("idEstudiante", UUID.randomUUID()))));

        final var pagina = repository.consultarEstudiantes(EJECUTOR, "ana", PROGRAMA, true, 2, 5);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_estudiantes_facultad_paginado"),
                eq(SpEstudianteFacultadPaginadoEntity.class));
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("numeroPagina", 2);
        verify(query).setParameter("tamanoPagina", 5);
        verify(query).setParameter("busqueda", "ana");
        verify(query).setParameter("idPrograma", PROGRAMA);
        verify(query).setParameter("estaActivo", true);
        assertEquals(21, pagina.total());
        assertEquals("1017000111", pagina.elementos().getFirst().numeroIdentificacion());
        assertTrue(pagina.elementos().getFirst().activo());
        assertNull(pagina.elementos().get(1).numeroIdentificacion());
        assertFalse(pagina.elementos().get(1).activo());
    }

    @Test
    void T_104_PAT_02_sin_filas_el_total_es_cero() {
        when(query.getResultList()).thenReturn(Collections.emptyList());

        assertEquals(0, repository.consultarEstudiantes(EJECUTOR, null, null, null, 1, 10).total());
        assertEquals(0, repository.consultarSesiones(EJECUTOR, null, null, null, 1, 10).total());
    }

    @Test
    void T_105_PAT_01_detalle_enlaza_estudiante_y_mapea_matriculas() {
        final UUID grupo = UUID.randomUUID();
        when(query.getResultList()).thenReturn(List.of(entidad(SpMatriculaEstudianteFacultadEntity.class, Map.of(
                "idGrupo", grupo, "idEstudiante", ESTUDIANTE, "numeroIdentificacion", 1017000111,
                "nombrePrograma", "Sistemas", "inpPlanEstudio", 20241, "estaActivoEstudiante", true))));

        final var matriculas = repository.consultarDetalleEstudiante(EJECUTOR, ESTUDIANTE);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_estudiante_facultad_detalle"),
                eq(SpMatriculaEstudianteFacultadEntity.class));
        verify(query).setParameter("idEstudiante", ESTUDIANTE);
        assertEquals(grupo, matriculas.getFirst().idGrupo());
        assertEquals(20241, matriculas.getFirst().inpPlanEstudio());
        assertTrue(matriculas.getFirst().activo());
    }

    @Test
    void T_119_PAT_01_estructura_mapea_niveles_y_banderas() {
        when(query.getResultList()).thenReturn(List.of(entidad(SpEstructuraFacultadEntity.class, Map.of(
                "clave", "f|p|pl", "idFacultad", UUID.randomUUID(), "nombreFacultad", "Ingenieria",
                "estaActivaFacultad", true, "idPrograma", PROGRAMA, "estaActivoPrograma", false,
                "inpPlanEstudio", 20241, "estaActivoPlanEstudio", true, "totalSemestres", 10))));

        final var filas = repository.consultarEstructura(EJECUTOR);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_estructura_facultad"),
                eq(SpEstructuraFacultadEntity.class));
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        assertTrue(filas.getFirst().facultadActiva());
        assertFalse(filas.getFirst().programaActivo());
        assertTrue(filas.getFirst().planActivo());
        assertEquals(10, filas.getFirst().totalSemestres());
    }

    @Test
    void T_145_PAT_01_sesiones_enlaza_rango_y_programa() {
        final LocalDate desde = LocalDate.of(2026, 2, 1);
        final LocalDate hasta = LocalDate.of(2026, 2, 28);
        when(query.getResultList()).thenReturn(List.of(entidad(SpSesionFacultadPaginadoEntity.class, Map.of(
                "id", UUID.randomUUID(), "codigo", "S-01", "nombrePrograma", "Sistemas",
                "fechaHoraInicio", LocalDateTime.of(2026, 2, 3, 7, 0), "totalRegistros", 4))));

        final var pagina = repository.consultarSesiones(EJECUTOR, desde, hasta, PROGRAMA, 1, 20);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_sesiones_facultad_paginado"),
                eq(SpSesionFacultadPaginadoEntity.class));
        verify(query).setParameter("fechaDesde", desde);
        verify(query).setParameter("fechaHasta", hasta);
        verify(query).setParameter("idPrograma", PROGRAMA);
        assertEquals(4, pagina.total());
        assertEquals("S-01", pagina.elementos().getFirst().codigo());
    }

    @Test
    void PAG_001_del_sp_se_traduce_a_validacion() {
        when(query.getResultList()).thenThrow(new PersistenceException("wrap",
                new RuntimeException("DBCODE=PAG_001|Paginacion invalida")));

        assertThrows(ValidationException.class, () -> repository.consultarEstudiantes(EJECUTOR, null, null, null, 1, 10));
    }

    @Test
    void fallo_tecnico_se_traduce_a_error_de_operacion() {
        when(query.getResultList()).thenThrow(new PersistenceException("timeout"));

        assertThrows(DatabaseOperationException.class, () -> repository.consultarEstructura(EJECUTOR));
    }

    private static <T> T entidad(final Class<T> tipo, final Map<String, Object> campos) {
        try {
            final var constructor = tipo.getDeclaredConstructor();
            constructor.setAccessible(true);
            final T instancia = constructor.newInstance();
            campos.forEach((campo, valor) -> ReflectionTestUtils.setField(instancia, campo, valor));
            return instancia;
        } catch (final ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
