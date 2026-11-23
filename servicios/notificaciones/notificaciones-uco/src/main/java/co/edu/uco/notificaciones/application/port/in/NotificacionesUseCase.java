package co.edu.uco.notificaciones.application.port.in;

import co.edu.uco.notificaciones.application.model.EstadoCanal;
import co.edu.uco.notificaciones.application.model.FiltroNotificaciones;
import co.edu.uco.notificaciones.application.model.PaginaNotificaciones;
import co.edu.uco.notificaciones.application.model.ResultadoRegistro;
import co.edu.uco.notificaciones.application.model.SolicitudNotificacion;
import co.edu.uco.notificaciones.domain.Notificacion;

import java.util.UUID;

/** Puerto de entrada: cola de notificaciones, correo de prueba y estado del canal. */
public interface NotificacionesUseCase {

    ResultadoRegistro registrar(SolicitudNotificacion solicitud);

    PaginaNotificaciones consultar(FiltroNotificaciones filtro, int pagina, int tamano);

    void reintentar(UUID id);

    Notificacion enviarCorreoPrueba(String tipo, String correoDestino, String solicitadoPor, String idCorrelacion);

    EstadoCanal estadoCanal();
}
