package co.edu.uco.notificaciones.application.port.out;

import co.edu.uco.notificaciones.domain.CorreoSaliente;

/** Puerto de salida: transporte del correo (SMTP o registro en log). */
public interface CorreoTransportPort {

    /** Falla permanente: {@code NoEntregableException}; cualquier otra excepcion se trata como transitoria. */
    void enviar(CorreoSaliente correo);

    /** Abre y cierra una conexion sin enviar nada. */
    default void verificarConexion() throws Exception {
        throw new UnsupportedOperationException("El transporte de correo no permite verificar la conexion.");
    }

    /** Si el transporte esta configurado para enviar (SMTP con host, o log). */
    boolean activo();
}
