package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.storage.resilience;

import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** REA-02: breaker del storage (MinIO): abre ante indisponibilidad y, abierto, falla rapido y cerrado. */
class CircuitBreakerFileStorageAdapterTest {

    private final FileStoragePort delegate = mock(FileStoragePort.class);
    private final CircuitBreaker breaker = CircuitBreaker.of("fileStorage", CircuitBreakerConfig.custom()
            .slidingWindowSize(2)
            .minimumNumberOfCalls(2)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofMinutes(1))
            .build());
    private final CircuitBreakerFileStorageAdapter adapter = new CircuitBreakerFileStorageAdapter(delegate, breaker);

    @Test
    void storage_caido_abre_el_breaker_y_luego_falla_sin_contactar_al_provider() {
        when(delegate.exists(anyString())).thenThrow(new FileStoragePort.StorageUnavailableException("caido", null));

        assertThrows(FileStoragePort.StorageUnavailableException.class, () -> adapter.exists("a"));
        assertThrows(FileStoragePort.StorageUnavailableException.class, () -> adapter.exists("b"));
        assertEquals(CircuitBreaker.State.OPEN, breaker.getState());

        final FileStoragePort.StorageUnavailableException abierto =
                assertThrows(FileStoragePort.StorageUnavailableException.class, () -> adapter.store(null));
        assertEquals(CircuitBreakerFileStorageAdapter.MENSAJE_ABIERTO, abierto.getMessage());
        assertThrows(FileStoragePort.StorageUnavailableException.class, () -> adapter.read("c"));
        assertThrows(FileStoragePort.StorageUnavailableException.class, () -> adapter.delete("d"));
        verify(delegate, times(2)).exists(anyString());
        verify(delegate, times(0)).store(any());
    }

    @Test
    void objeto_inexistente_es_respuesta_sana_y_no_abre_el_breaker() {
        when(delegate.read(anyString())).thenThrow(new FileStoragePort.ObjectNotFoundException("x"));

        for (int i = 0; i < 4; i++) {
            assertThrows(FileStoragePort.ObjectNotFoundException.class, () -> adapter.read("x"));
        }
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
    }

    @Test
    void llamadas_exitosas_pasan_el_resultado_del_provider() {
        when(delegate.exists("si")).thenReturn(true);

        assertTrue(adapter.exists("si"));
        assertFalse(adapter.exists("no"));
        adapter.delete("z");
        verify(delegate).delete("z");
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
    }

    @Test
    void errores_de_programacion_no_cuentan_como_caida_del_provider() {
        doThrow(new IllegalArgumentException("fileId invalido")).when(delegate).delete(anyString());

        for (int i = 0; i < 4; i++) {
            assertThrows(IllegalArgumentException.class, () -> adapter.delete("?"));
        }
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
        assertEquals(0, breaker.getMetrics().getNumberOfBufferedCalls());
    }
}
