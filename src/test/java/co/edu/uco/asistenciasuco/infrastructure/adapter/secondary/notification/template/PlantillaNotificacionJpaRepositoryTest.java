package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.StoredProcedureQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlantillaNotificacionJpaRepositoryTest {

    private EntityManager entityManager;
    private MutableClock clock;
    private PlantillaNotificacionJpaRepository repository;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        clock = new MutableClock(Instant.parse("2026-10-29T12:00:00Z"));
        repository = new PlantillaNotificacionJpaRepository(entityManager, clock, Duration.ofSeconds(60));
    }

    private StoredProcedureQuery mockSpQuery() {
        final StoredProcedureQuery sp = mock(StoredProcedureQuery.class);
        when(entityManager.createStoredProcedureQuery("dbo.usp_consultar_plantilla_notificacion_activa")).thenReturn(sp);
        when(sp.registerStoredProcedureParameter(anyString(), any(), any())).thenReturn(sp);
        when(sp.setParameter(anyString(), any())).thenReturn(sp);
        return sp;
    }

    @Test
    @DisplayName("BE-T01: mapeo posicional de usp_consultar_plantilla_notificacion_activa (3 result sets) y 0 filas -> Optional.empty()")
    void BE_T01_mapeo_tres_result_sets_y_cero_filas() {
        final StoredProcedureQuery sp = mockSpQuery();
        final UUID corrId = UUID.randomUUID();
        final Object[] canonicalOk = new Object[]{corrId, "OK", "DBCODE=GEN_003|OK", Boolean.TRUE};
        final Object[] plantillaRow = new Object[]{1, "Retiro del grupo {grupo}", "Se registró el retiro de {estudiante} del grupo {grupo}."};
        final Object[] m1 = new Object[]{"estudiante", Boolean.TRUE};
        final Object[] m2 = new Object[]{"grupo", 1};
        final Object[] m3 = new Object[]{"motivo", Boolean.FALSE};

        when(sp.execute()).thenReturn(true);
        when(sp.hasMoreResults()).thenReturn(true, true, false);
        when(sp.getResultList())
                .thenReturn(List.of((Object) canonicalOk))
                .thenReturn(List.of((Object) plantillaRow))
                .thenReturn(List.of(m1, m2, m3));

        final Optional<PlantillaNotificacion> res = repository.consultarActiva("RETIRO_ESTUDIANTE_GRUPO", "es");
        assertTrue(res.isPresent());
        assertEquals(1, res.get().version());
        assertEquals("Retiro del grupo {grupo}", res.get().asunto());
        assertEquals(3, res.get().marcadores().size());
        assertTrue(res.get().marcadores().contains(new PlantillaNotificacion.Marcador("estudiante", true)));
        assertTrue(res.get().marcadores().contains(new PlantillaNotificacion.Marcador("grupo", true)));
        assertTrue(res.get().marcadores().contains(new PlantillaNotificacion.Marcador("motivo", false)));

        // Caso 0 filas en result set 2 -> Optional.empty()
        final StoredProcedureQuery spVacio = mock(StoredProcedureQuery.class);
        when(entityManager.createStoredProcedureQuery("dbo.usp_consultar_plantilla_notificacion_activa")).thenReturn(spVacio);
        when(spVacio.registerStoredProcedureParameter(anyString(), any(), any())).thenReturn(spVacio);
        when(spVacio.setParameter(anyString(), any())).thenReturn(spVacio);
        when(spVacio.execute()).thenReturn(true);
        when(spVacio.hasMoreResults()).thenReturn(true, true, false);
        when(spVacio.getResultList())
                .thenReturn(List.of((Object) canonicalOk))
                .thenReturn(List.of())
                .thenReturn(List.of());

        final Optional<PlantillaNotificacion> vacio = repository.consultarActiva("TIPO_INEXISTENTE", "es");
        assertFalse(vacio.isPresent());
    }

    @Test
    @DisplayName("BE-T02: cache por (tipo, idioma) con TTL: segunda lectura no vuelve a la BD; vencido el TTL, si")
    void BE_T02_cache_por_tipo_idioma_con_ttl() {
        final StoredProcedureQuery sp = mockSpQuery();
        final Object[] canonicalOk = new Object[]{UUID.randomUUID(), "OK", "DBCODE=GEN_003|OK", Boolean.TRUE};
        final Object[] plantillaV1 = new Object[]{1, "Asunto v1 {grupo}", "Cuerpo v1 {grupo}"};
        final Object[] plantillaV2 = new Object[]{2, "Asunto v2 {grupo}", "Cuerpo v2 {grupo}"};
        final Object[] marcadorGrupo = new Object[]{"grupo", Boolean.TRUE};

        when(sp.execute()).thenReturn(true);
        when(sp.hasMoreResults()).thenReturn(true, true, false, true, true, false);
        when(sp.getResultList())
                .thenReturn(List.of((Object) canonicalOk))
                .thenReturn(List.of((Object) plantillaV1))
                .thenReturn(List.of((Object) marcadorGrupo))
                .thenReturn(List.of((Object) canonicalOk))
                .thenReturn(List.of((Object) plantillaV2))
                .thenReturn(List.of((Object) marcadorGrupo));

        final Optional<PlantillaNotificacion> primera = repository.consultarActiva("SOLICITUD_INGRESO_REGISTRADA", "es");
        assertTrue(primera.isPresent());
        assertEquals(1, primera.get().version());

        // Avanzar 30s (dentro de TTL de 60s): no debe consultar BD
        clock.advance(Duration.ofSeconds(30));
        final Optional<PlantillaNotificacion> segunda = repository.consultarActiva("SOLICITUD_INGRESO_REGISTRADA", "es");
        assertTrue(segunda.isPresent());
        assertEquals(1, segunda.get().version());
        verify(entityManager, times(1)).createStoredProcedureQuery(eq("dbo.usp_consultar_plantilla_notificacion_activa"));

        // Avanzar otros 31s (TTL vencido): vuelve a consultar BD y obtiene v2
        clock.advance(Duration.ofSeconds(31));
        final Optional<PlantillaNotificacion> tercera = repository.consultarActiva("SOLICITUD_INGRESO_REGISTRADA", "es");
        assertTrue(tercera.isPresent());
        assertEquals(2, tercera.get().version());
        verify(entityManager, times(2)).createStoredProcedureQuery(eq("dbo.usp_consultar_plantilla_notificacion_activa"));
    }

    @Test
    @DisplayName("BE-T01b: rechazo del SP (estadoResultado=0) o PersistenceException lanza DatabaseOperationException")
    void BE_T01b_error_sp_o_jpa_lanza_excepcion() {
        final StoredProcedureQuery spError = mockSpQuery();
        final Object[] canonicalFail = new Object[]{UUID.randomUUID(), "Datos invalidos", "DBCODE=NTF_001|Invalido", Boolean.FALSE};
        when(spError.execute()).thenReturn(true);
        when(spError.getResultList()).thenReturn(List.of((Object) canonicalFail));

        assertThrows(DatabaseOperationException.class,
                () -> repository.consultarActiva("INVALIDO", "es"));

        when(entityManager.createStoredProcedureQuery(anyString())).thenThrow(new PersistenceException("DB down"));
        assertThrows(DatabaseOperationException.class,
                () -> repository.consultarActiva("OTRO_TIPO", "es"));

        assertThrows(IllegalArgumentException.class,
                () -> new PlantillaNotificacionJpaRepository(entityManager, clock, Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new PlantillaNotificacionJpaRepository(entityManager, clock, Duration.ofSeconds(-5)));
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> current;

        private MutableClock(final Instant initial) {
            this.current = new AtomicReference<>(initial);
        }

        void advance(final Duration duration) {
            current.updateAndGet(now -> now.plus(duration));
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current.get();
        }
    }
}
