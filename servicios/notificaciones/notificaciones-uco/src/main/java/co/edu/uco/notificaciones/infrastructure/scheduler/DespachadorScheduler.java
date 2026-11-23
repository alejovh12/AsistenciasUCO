package co.edu.uco.notificaciones.infrastructure.scheduler;

import co.edu.uco.notificaciones.application.port.out.DespertadorPort;
import co.edu.uco.notificaciones.application.service.Despachador;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Hilo de fondo del despachador: barre la cola cada {@code sweeperInterval} o apenas llega trabajo ({@link #despertar()}).
 * Un ciclo que falla no detiene el hilo.
 */
public class DespachadorScheduler implements SmartLifecycle, DespertadorPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(DespachadorScheduler.class);

    private final Despachador despachador;
    private final AtomicBoolean ejecutando = new AtomicBoolean(false);
    private final Object monitor = new Object();
    private boolean senal;
    private Thread hilo;

    public DespachadorScheduler(final Despachador despachador) {
        this.despachador = Objects.requireNonNull(despachador, "Despachador es obligatorio.");
    }

    @Override
    public void despertar() {
        synchronized (monitor) {
            senal = true;
            monitor.notifyAll();
        }
    }

    @Override
    public void start() {
        if (!despachador.configuracion().habilitado()) {
            LOGGER.info("Despachador deshabilitado por configuracion (DISPATCHER_ENABLED=false).");
            return;
        }
        if (ejecutando.compareAndSet(false, true)) {
            despachador.depurar();
            hilo = new Thread(this::bucle, "notificaciones-despachador-" + despachador.configuracion().instanceId());
            hilo.setDaemon(true);
            hilo.start();
            LOGGER.info("Despachador iniciado para la instancia {}.", despachador.configuracion().instanceId());
        }
    }

    @Override
    public void stop() {
        if (ejecutando.compareAndSet(true, false)) {
            despertar();
            if (hilo != null) {
                try {
                    hilo.join(TimeUnit.SECONDS.toMillis(5));
                } catch (final InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
            LOGGER.info("Despachador detenido.");
        }
    }

    @Override
    public boolean isRunning() {
        return ejecutando.get();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }

    private void bucle() {
        while (ejecutando.get()) {
            try {
                despachador.ejecutarCiclo(ejecutando::get);
                despachador.depurarSiCorresponde();
            } catch (final RuntimeException ex) {
                LOGGER.error("Error inesperado en el ciclo del despachador: {}", ex.getMessage(), ex);
            }
            if (!esperar()) {
                break;
            }
        }
    }

    /** @return false si el hilo fue interrumpido. */
    private boolean esperar() {
        synchronized (monitor) {
            if (ejecutando.get() && !senal) {
                try {
                    monitor.wait(despachador.configuracion().sweeperInterval().toMillis());
                } catch (final InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            senal = false;
            return true;
        }
    }
}
