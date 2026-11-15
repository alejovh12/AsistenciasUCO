package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * VAL-oleada7 (F12-06 / REA-03): con dos réplicas, editar un parámetro o un mensaje solo invalidaba la caché de la réplica
 * que atendió la edición; la otra servía el valor viejo hasta reiniciar. Las cachés de los proveedores SQL vencen ahora
 * después de un TTL, así que todas las réplicas convergen solas.
 */
class CatalogoSqlCacheVencimientoTest {

    private static final Duration TTL = Duration.ofSeconds(60);

    private final EntityManager manager = mock(EntityManager.class);
    private final RelojManual reloj = new RelojManual(Instant.parse("2026-11-15T12:00:00Z"));

    @SuppressWarnings("unchecked")
    private TypedQuery<String> consultaQueDevuelve(final String primero, final String segundo) {
        final TypedQuery<String> query = mock(TypedQuery.class);
        when(manager.createQuery(anyString(), eq(String.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(primero), List.of(segundo));
        return query;
    }

    @Test
    void parametro_vigente_se_sirve_de_cache_y_vencido_se_vuelve_a_leer() {
        consultaQueDevuelve("20", "25");
        final ParameterCatalogJpaRepository catalogo = new ParameterCatalogJpaRepository(manager, reloj, TTL);

        assertEquals(Optional.of("20"), catalogo.getParameter("ASISTENCIA", "UMBRAL"));
        reloj.avanzar(TTL.minusSeconds(1));
        assertEquals(Optional.of("20"), catalogo.getParameter("ASISTENCIA", "UMBRAL"), "dentro del TTL no consulta");
        verify(manager, times(1)).createQuery(anyString(), eq(String.class));

        reloj.avanzar(Duration.ofSeconds(1));
        assertEquals(Optional.of("25"), catalogo.getParameter("ASISTENCIA", "UMBRAL"), "vencido toma el valor nuevo");
        verify(manager, times(2)).createQuery(anyString(), eq(String.class));
    }

    @Test
    void invalidacion_explicita_de_parametros_sigue_funcionando_dentro_del_ttl() {
        consultaQueDevuelve("20", "25");
        final ParameterCatalogJpaRepository catalogo = new ParameterCatalogJpaRepository(manager, reloj, TTL);

        assertEquals(Optional.of("20"), catalogo.getParameter("ASISTENCIA", "UMBRAL"));
        catalogo.invalidateParameter("ASISTENCIA", "UMBRAL");
        assertEquals(Optional.of("25"), catalogo.getParameter("ASISTENCIA", "UMBRAL"));
    }

    @Test
    void mensaje_de_usuario_vigente_se_sirve_de_cache_y_vencido_se_vuelve_a_leer() {
        consultaQueDevuelve("Texto viejo", "Texto nuevo");
        final MessageCatalogJpaRepository catalogo = new MessageCatalogJpaRepository(manager, reloj, TTL);

        assertEquals("Texto viejo", catalogo.getUserMessage("MSG_X"));
        reloj.avanzar(TTL.minusSeconds(1));
        assertEquals("Texto viejo", catalogo.getUserMessage("MSG_X"));
        verify(manager, times(1)).createQuery(anyString(), eq(String.class));

        reloj.avanzar(Duration.ofSeconds(1));
        assertEquals("Texto nuevo", catalogo.getUserMessage("MSG_X"));
    }

    @Test
    void mensaje_tecnico_vencido_se_vuelve_a_leer() {
        consultaQueDevuelve("Tecnico viejo", "Tecnico nuevo");
        final MessageCatalogJpaRepository catalogo = new MessageCatalogJpaRepository(manager, reloj, TTL);

        assertEquals("Tecnico viejo", catalogo.getTechnicalMessage("TEC_X"));
        reloj.avanzar(TTL);
        assertEquals("Tecnico nuevo", catalogo.getTechnicalMessage("TEC_X"));
    }

    @Test
    void ttl_fuera_de_rango_se_rechaza_al_construir() {
        for (final Duration invalido : new Duration[]{Duration.ZERO, Duration.ofSeconds(-1), Duration.ofHours(25)}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new ParameterCatalogJpaRepository(manager, reloj, invalido), invalido.toString());
            assertThrows(IllegalArgumentException.class,
                    () -> new MessageCatalogJpaRepository(manager, reloj, invalido), invalido.toString());
        }
        assertThrows(NullPointerException.class, () -> new ParameterCatalogJpaRepository(manager, null, TTL));
        assertThrows(NullPointerException.class, () -> new MessageCatalogJpaRepository(manager, reloj, null));
    }

    /** Reloj que solo avanza cuando la prueba lo pide. */
    private static final class RelojManual extends Clock {
        private Instant ahora;

        private RelojManual(final Instant inicio) {
            this.ahora = inicio;
        }

        void avanzar(final Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }
}
