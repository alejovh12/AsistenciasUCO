package co.edu.uco.asistenciasuco.application.features.asistenciaqr.common;

import java.util.concurrent.ThreadLocalRandom;

/**
 * CULM-001 B.2 (3.2.7): espera entre reintentos tras un interbloqueo. Retroceso exponencial con jitter completo: en el
 * intento n la espera es aleatoria entre {@link #MINIMO_MS} y min({@link #MAXIMO_MS}, 40 ms * 2^(n-1)); con el tope de 250
 * ms y 8 reintentos la espera acumulada maxima es de 1,53 s. Es una interfaz para que las pruebas no duerman.
 */
@FunctionalInterface
public interface EsperaReintento {

    long MINIMO_MS = 20;
    long MAXIMO_MS = 250;
    long BASE_VENTANA_MS = 40;

    void esperar(long milisegundos);

    /** Espera antes del reintento {@code intento} (1 = primero): aleatoria en [MINIMO_MS, ventana del intento]. */
    default void esperarReintento(final int intento) {
        esperar(ThreadLocalRandom.current().nextLong(MINIMO_MS, ventanaMaximaMs(intento) + 1));
    }

    /** Limite superior de la espera del intento: 40, 80, 160 y luego 250 ms. */
    static long ventanaMaximaMs(final int intento) {
        final int exponente = Math.max(0, Math.min(intento - 1, 10));
        return Math.min(MAXIMO_MS, BASE_VENTANA_MS << exponente);
    }

    static EsperaReintento delHilo() {
        return milisegundos -> {
            try {
                Thread.sleep(milisegundos);
            } catch (final InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        };
    }
}
