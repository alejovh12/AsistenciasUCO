package co.edu.uco.notificaciones.infrastructure.observability;

import co.edu.uco.notificaciones.application.port.out.MetricasPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class MicrometerMetricas implements MetricasPort {

    private final MeterRegistry registro;

    public MicrometerMetricas(final MeterRegistry registro) {
        this.registro = registro;
    }

    @Override
    public void contar(final String nombre, final double cantidad, final String... tags) {
        Counter.builder(nombre).tags(Tags.of(tags)).register(registro).increment(cantidad);
    }

    @Override
    public void medir(final String nombre, final long nanos, final String... tags) {
        Timer.builder(nombre).tags(Tags.of(tags)).register(registro).record(nanos, TimeUnit.NANOSECONDS);
    }
}
