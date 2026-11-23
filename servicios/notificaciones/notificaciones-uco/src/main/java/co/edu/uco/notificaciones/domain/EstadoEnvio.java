package co.edu.uco.notificaciones.domain;

/** Estados del ciclo de vida de una notificacion en la cola de envio. */
public enum EstadoEnvio {
    PENDIENTE("Pendiente", false),
    RESERVADA("Reservada", false),
    FALLIDA_REINTENTABLE("Fallida reintentable", false),
    ENTREGADA("Entregada", true),
    FALLIDA_DEFINITIVA("Fallida definitiva", true);

    private final String nombre;
    private final boolean terminal;

    EstadoEnvio(final String nombre, final boolean terminal) {
        this.nombre = nombre;
        this.terminal = terminal;
    }

    public String nombre() {
        return nombre;
    }

    public boolean esTerminal() {
        return terminal;
    }
}
