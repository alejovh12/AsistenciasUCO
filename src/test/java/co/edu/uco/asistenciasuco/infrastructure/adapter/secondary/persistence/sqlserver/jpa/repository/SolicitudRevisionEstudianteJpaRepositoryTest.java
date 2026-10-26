package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpSolicitudRevisionEstudianteEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

/** HU010/HU011 (consulta): native EXEC del SP del estudiante con binding nombrado y semantica UTC v2 (RP-01..RP-04). */
class SolicitudRevisionEstudianteJpaRepositoryTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID EJECUTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private SolicitudRevisionEstudianteJpaRepository repository;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        repository = new SolicitudRevisionEstudianteJpaRepository(entityManager);
        when(entityManager.createNativeQuery(anyString(), any(Class.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void constructor_rechaza_entity_manager_nulo() {
        assertThrows(NullPointerException.class, () -> new SolicitudRevisionEstudianteJpaRepository(null));
    }

    @Test
    void RP01_enlaza_ejecutor_correlacion_estado_y_paginacion() {
        when(query.getResultList()).thenReturn(Collections.emptyList());

        assertTrue(repository.consultarSolicitudesEstudiante(EJECUTOR, "A", 2, 5).isEmpty());

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_solicitudes_revision_estudiante"),
                eq(SpSolicitudRevisionEstudianteEntity.class));
        verify(query).setParameter("idUsuarioEjecutor", EJECUTOR);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("codigoEstado", "A");
        verify(query).setParameter("numeroPagina", 2);
        verify(query).setParameter("tamanoPagina", 5);
    }

    @Test
    void RP02_fecha_de_sesion_utc_solo_con_procedencia_verificable() {
        final LocalDateTime inicio = LocalDateTime.of(2026, 10, 19, 15, 30, 45);
        final LocalDateTime soporte = LocalDateTime.of(2026, 10, 20, 10, 0);
        final UUID archivo = UUID.randomUUID();
        when(query.getResultList()).thenReturn(List.of(
                fila(Map.of("idSolicitudRevisionAsistencia", UUID.randomUUID(), "codigoEstado", "P",
                        "fechaSolicitud", LocalDate.of(2026, 10, 20), "fechaHoraInicioSesion", inicio,
                        "procedenciaTemporalSesion", "UTC_V2", "idArchivoSoporte", archivo,
                        "tamanioSoporteBytes", 2048L, "fechaRegistroSoporteUtc", soporte, "totalRegistros", 2)),
                fila(Map.of("idSolicitudRevisionAsistencia", UUID.randomUUID(), "codigoEstado", "A",
                        "fechaHoraInicioSesion", inicio, "totalRegistros", 2))));

        final var filas = repository.consultarSolicitudesEstudiante(EJECUTOR, null, 1, 10);

        assertEquals(Instant.parse("2026-10-19T15:30:45Z"), filas.getFirst().fechaHoraInicioSesionUtc());
        assertEquals("UTC_V2", filas.getFirst().procedenciaTemporalSesion());
        assertEquals(archivo, filas.getFirst().idArchivoSoporte());
        assertEquals(Instant.parse("2026-10-20T10:00:00Z"), filas.getFirst().fechaRegistroSoporteUtc());
        assertEquals(2, filas.getFirst().totalRegistros());
        assertNull(filas.get(1).fechaHoraInicioSesionUtc(), "Sin procedencia no se afirma un instante UTC");
        assertNull(filas.get(1).idArchivoSoporte());
    }

    @Test
    void RP03_total_nulo_se_lee_como_cero() {
        when(query.getResultList()).thenReturn(List.of(fila(Map.of("idSolicitudRevisionAsistencia", UUID.randomUUID()))));

        assertEquals(0, repository.consultarSolicitudesEstudiante(EJECUTOR, null, 1, 10).getFirst().totalRegistros());
    }

    @Test
    void RP04_fallo_tecnico_se_traduce_a_error_de_operacion() {
        when(query.getResultList()).thenThrow(new PersistenceException("timeout"));

        assertThrows(DatabaseOperationException.class, () -> repository.consultarSolicitudesEstudiante(EJECUTOR, null, 1, 10));
    }

    private static SpSolicitudRevisionEstudianteEntity fila(final Map<String, Object> campos) {
        try {
            final var constructor = SpSolicitudRevisionEstudianteEntity.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            final SpSolicitudRevisionEstudianteEntity entidad = constructor.newInstance();
            campos.forEach((campo, valor) -> ReflectionTestUtils.setField(entidad, campo, valor));
            return entidad;
        } catch (final ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
