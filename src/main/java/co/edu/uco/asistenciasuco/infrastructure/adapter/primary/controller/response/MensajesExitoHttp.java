package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response;

/**
 * Mensajes de exito de respaldo de los controladores cuya operacion no devuelve un texto del catalogo (el caso de uso es
 * {@code void}). Viven en un solo lugar y con ortografia institucional; cuando la BD siembre las filas de confirmacion
 * correspondientes se resuelven por {@code MessageCatalogPort} y estos textos quedan como respaldo.
 */
public final class MensajesExitoHttp {

    public static final String CIERRE_MASIVO_SOLICITADO = "Cierre masivo solicitado correctamente.";
    public static final String SOLICITUD_REVISION_REGISTRADA = "Solicitud de revisión registrada correctamente.";
    public static final String ASISTENCIAS_SESION_REGISTRADAS = "Asistencias de la sesión registradas correctamente.";
    public static final String SESION_CERRADA = "Sesión cerrada correctamente.";
    public static final String SESION_CREADA = "Sesión creada correctamente.";
    public static final String SESIONES_GRUPO_GENERADAS = "Sesiones del grupo generadas correctamente.";
    public static final String EVENTO_EMITIDO = "Evento reactivo emitido a %d suscriptores.";

    private MensajesExitoHttp() {
    }
}
