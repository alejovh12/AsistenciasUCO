package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpAsistenciaSesionPaginadoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpGrupoProgramaPaginadoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpSesionGrupoPaginadoEntity;
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

/** HU064/HU060/HU061: native EXEC de los SP paginados de lectura del coordinador. */
class AsistenciaProgramaJpaRepositoryTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID EJECUTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PERIODO = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID GRUPO = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID SESION = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private AsistenciaProgramaJpaRepository repository;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        repository = new AsistenciaProgramaJpaRepository(entityManager);
        when(entityManager.createNativeQuery(anyString(), any(Class.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void constructor_rechaza_entity_manager_nulo() {
        assertThrows(NullPointerException.class, () -> new AsistenciaProgramaJpaRepository(null));
    }

    @Test
    void T_064_PAT_01_grupos_enlaza_filtros_y_mapea_total() {
        when(query.getResultList()).thenReturn(List.of(entidad(SpGrupoProgramaPaginadoEntity.class, Map.ofEntries(
                Map.entry("id", GRUPO), Map.entry("codigo", 3), Map.entry("nombre", "Grupo 3"),
                Map.entry("nombreAsignatura", "Calculo"), Map.entry("capacidadMaximaPermitida", 30),
                Map.entry("estudiantesActivos", 25), Map.entry("cuposDisponibles", 5),
                Map.entry("grupoEstaHablitado", true),
                Map.entry("fechaInicioPeriodoAcademico", LocalDate.of(2026, 1, 20)),
                Map.entry("totalRegistros", 14)))));

        final var pagina = repository.consultarGrupos(EJECUTOR, PERIODO, "calc", 2, 5);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_grupos_paginado"),
                eq(SpGrupoProgramaPaginadoEntity.class));
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idPeriodoAcademico", PERIODO);
        verify(query).setParameter("filtroTexto", "calc");
        verify(query).setParameter("numeroPagina", 2);
        verify(query).setParameter("tamanoPagina", 5);
        assertEquals(14, pagina.total());
        final var grupo = pagina.elementos().getFirst();
        assertEquals(GRUPO, grupo.id());
        assertEquals("Calculo", grupo.nombreAsignatura());
        assertEquals(5, grupo.cuposDisponibles());
        assertTrue(grupo.habilitado());
        assertEquals(LocalDate.of(2026, 1, 20), grupo.inicioPeriodo());
    }

    @Test
    void T_064_PAT_02_sin_filas_el_total_es_cero() {
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final var pagina = repository.consultarGrupos(EJECUTOR, null, null, 1, 10);

        assertEquals(0, pagina.total());
        assertTrue(pagina.elementos().isEmpty());
    }

    @Test
    void T_060_PAT_01_sesiones_enlaza_grupo_y_rango() {
        final LocalDate desde = LocalDate.of(2026, 2, 1);
        final LocalDate hasta = LocalDate.of(2026, 2, 28);
        when(query.getResultList()).thenReturn(List.of(entidad(SpSesionGrupoPaginadoEntity.class, Map.of(
                "id", SESION, "idGrupo", GRUPO, "codigo", "S-01", "numeroSemana", 2,
                "fechaHoraInicio", LocalDateTime.of(2026, 2, 3, 7, 0)))));

        final var pagina = repository.consultarSesionesGrupo(EJECUTOR, GRUPO, desde, hasta, 1, 10);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_sesiones_grupo_paginado"),
                eq(SpSesionGrupoPaginadoEntity.class));
        verify(query).setParameter("idGrupo", GRUPO);
        verify(query).setParameter("fechaDesde", desde);
        verify(query).setParameter("fechaHasta", hasta);
        assertEquals(0, pagina.total());
        assertEquals("S-01", pagina.elementos().getFirst().codigo());
        assertEquals(2, pagina.elementos().getFirst().numeroSemana());
    }

    @Test
    void T_061_PAT_01_asistencias_enlaza_sesion_y_filtro_y_mapea_estudiante() {
        when(query.getResultList()).thenReturn(List.of(
                entidad(SpAsistenciaSesionPaginadoEntity.class, Map.of(
                        "idDetalleAsistencia", UUID.randomUUID(), "idSesion", SESION, "numeroIdentificacion", 1017000111,
                        "nombreCompleto", "Ana Ruiz", "asistio", true, "totalRegistros", 2)),
                entidad(SpAsistenciaSesionPaginadoEntity.class, Map.of(
                        "idDetalleAsistencia", UUID.randomUUID(), "idSesion", SESION, "codigoRazonCausa", "INJ",
                        "totalRegistros", 2))));

        final var pagina = repository.consultarAsistenciasSesion(EJECUTOR, SESION, true, 1, 10);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_asistencias_sesion_paginado"),
                eq(SpAsistenciaSesionPaginadoEntity.class));
        verify(query).setParameter("idSesion", SESION);
        verify(query).setParameter("asistio", true);
        assertEquals(2, pagina.total());
        assertEquals("1017000111", pagina.elementos().getFirst().numeroIdentificacion());
        assertTrue(pagina.elementos().getFirst().asistio());
        assertNull(pagina.elementos().get(1).numeroIdentificacion());
        assertFalse(pagina.elementos().get(1).asistio());
        assertEquals("INJ", pagina.elementos().get(1).codigoRazonCausa());
    }

    @Test
    void PAG_001_del_sp_se_traduce_a_validacion() {
        when(query.getResultList()).thenThrow(new PersistenceException("wrap",
                new RuntimeException("DBCODE=PAG_001|Paginacion invalida")));

        assertThrows(ValidationException.class, () -> repository.consultarGrupos(EJECUTOR, null, null, 1, 10));
    }

    @Test
    void fallo_tecnico_se_traduce_a_error_de_operacion() {
        when(query.getResultList()).thenThrow(new PersistenceException("timeout"));

        assertThrows(DatabaseOperationException.class,
                () -> repository.consultarAsistenciasSesion(EJECUTOR, SESION, null, 1, 10));
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
