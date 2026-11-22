package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response;

/**
 * Confirmaciones de los controladores cuya operacion no devuelve un texto del catalogo (el caso de uso es {@code void}). Cada una
 * tiene su codigo {@code OK_*} sembrado en el catalogo de mensajes de la BD (seed 74); {@link MensajesExito} lo resuelve por
 * {@code MessageCatalogPort} y el texto de aqui queda como respaldo cuando el catalogo no responde. Mismo texto que el seed y mismo
 * formato de marcadores que el catalogo ({@code {0}} posicional).
 */
public enum MensajesExitoHttp {

    CIERRE_MASIVO_SOLICITADO("OK_ADMIN_001", "Cierre masivo solicitado correctamente."),
    SOLICITUD_REVISION_REGISTRADA("OK_ASISTENCIA_001", "Solicitud de revisión registrada correctamente."),
    ASISTENCIAS_SESION_REGISTRADAS("OK_ASISTENCIA_002", "Asistencias de la sesión registradas correctamente."),
    SESION_CERRADA("OK_SESION_001", "Sesión cerrada correctamente."),
    SESION_CREADA("OK_SESION_002", "Sesión creada correctamente."),
    SESIONES_GRUPO_GENERADAS("OK_SESION_003", "Sesiones del grupo generadas correctamente."),
    /** {@code {0}} = numero de suscriptores activos. */
    EVENTO_EMITIDO("OK_REALTIME_001", "Evento reactivo emitido a {0} suscriptores.");

    private final String codigo;
    private final String respaldo;

    MensajesExitoHttp(final String codigo, final String respaldo) {
        this.codigo = codigo;
        this.respaldo = respaldo;
    }

    public String codigo() {
        return codigo;
    }

    public String respaldo() {
        return respaldo;
    }
}
