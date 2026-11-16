package co.edu.uco.asistenciasuco.application.features.catalogo.consultarmensajeinterfaz.primaryports;

/**
 * INT-003 (MessageUcoLab 0.3.0, paso 12): puerto de entrada del texto de la interfaz que la SPA pide por su codigo
 * ({@code UI###}). La SPA ya no puede leer MessageUcoLab directamente (la ruta publica se retiro y la identidad de
 * servicio no puede vivir en el navegador); lo lee a traves del Backend.
 */
public interface ConsultarMensajeInterfazInputPort {

    /**
     * @param codigo codigo del texto de la interfaz (ej. {@code UI012}).
     * @return el texto vigente del catalogo de mensajes.
     */
    MensajeInterfaz execute(String codigo);

    /** Texto de la interfaz listo para mostrar; los marcadores ({@code {nombre}}) los llena la SPA. */
    record MensajeInterfaz(String codigo, String contenido) {
    }
}
