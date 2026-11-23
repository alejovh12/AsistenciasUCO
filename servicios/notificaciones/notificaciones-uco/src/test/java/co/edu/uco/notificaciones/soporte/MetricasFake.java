package co.edu.uco.notificaciones.soporte;

import co.edu.uco.notificaciones.application.port.out.MetricasPort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Registra las metricas emitidas como "nombre|tag=valor". */
public class MetricasFake implements MetricasPort {

    public final List<String> registros = new ArrayList<>();

    @Override
    public void contar(final String nombre, final double cantidad, final String... tags) {
        registros.add(nombre + "|" + String.join(",", tags) + "|" + cantidad);
    }

    @Override
    public void medir(final String nombre, final long nanos, final String... tags) {
        registros.add("T:" + nombre + "|" + String.join(",", tags));
    }

    public boolean contiene(final String fragmento) {
        return registros.stream().anyMatch(r -> r.contains(fragmento));
    }

    public long cuenta(final String fragmento) {
        return registros.stream().filter(r -> r.contains(fragmento)).count();
    }

    @Override
    public String toString() {
        return Arrays.toString(registros.toArray());
    }
}
