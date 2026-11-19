package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.domain;

/**
 * HU074 (migracion 57): codigos del catalogo de mensajes con los que la DB confirma el cambio de estado de una asignatura.
 * El texto de aqui es solo el respaldo cuando ni el SP ni el catalogo entregan el mensaje.
 */
public enum MensajeEstadoAsignatura {
    ASI_011("La asignatura quedó activa."),
    ASI_012("La asignatura quedó inactiva.");

    private final String respaldo;

    MensajeEstadoAsignatura(final String respaldo) {
        this.respaldo = respaldo;
    }

    public static MensajeEstadoAsignatura para(final boolean estaActiva) {
        return estaActiva ? ASI_011 : ASI_012;
    }

    public String codigo() {
        return name();
    }

    public String respaldo() {
        return respaldo;
    }
}
