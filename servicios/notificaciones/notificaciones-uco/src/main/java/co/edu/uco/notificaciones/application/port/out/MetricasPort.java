package co.edu.uco.notificaciones.application.port.out;

/** Puerto de salida: metricas de negocio (Micrometer en infraestructura). {@code tags} son pares clave, valor. */
public interface MetricasPort {

    void contar(String nombre, double cantidad, String... tags);

    void medir(String nombre, long nanos, String... tags);
}
