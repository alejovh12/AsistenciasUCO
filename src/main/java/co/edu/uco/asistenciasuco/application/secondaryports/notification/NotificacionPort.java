package co.edu.uco.asistenciasuco.application.secondaryports.notification;

/**
 * Puerto de salida hacia el componente institucional de notificaciones (correo, WhatsApp, SMS...). Mientras ese
 * componente no este integrado, el adaptador por defecto solo registra el aviso en el log. El envio es de mejor
 * esfuerzo: los casos de uso no deben revertir su operacion de negocio si el aviso falla.
 */
public interface NotificacionPort {

    void notificar(Notificacion notificacion);
}
