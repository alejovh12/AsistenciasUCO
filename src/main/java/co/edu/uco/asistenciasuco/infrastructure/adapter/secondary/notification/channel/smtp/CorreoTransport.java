package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.channel.smtp;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionNoEntregableException;

/**
 * Abstraccion de transporte SMTP para envio de mensajes multipart.
 */
public interface CorreoTransport {

    /**
     * Envia el correo saliente a traves del servidor SMTP configurado.
     *
     * @param correo datos del mensaje saliente
     * @throws NotificacionNoEntregableException si el servidor SMTP rechaza de forma permanente (5xx / direccion invalida)
     */
    void enviar(CorreoSaliente correo) throws NotificacionNoEntregableException;

    /**
     * OBS-10: abre y cierra una conexion con el servidor SMTP (autenticandose si hay credenciales) sin enviar nada.
     *
     * @throws Exception si el servidor no responde o rechaza la conexion
     * @throws UnsupportedOperationException si el transporte no sabe verificar su conexion
     */
    default void verificarConexion() throws Exception {
        throw new UnsupportedOperationException("El transporte de correo no permite verificar la conexion.");
    }
}
