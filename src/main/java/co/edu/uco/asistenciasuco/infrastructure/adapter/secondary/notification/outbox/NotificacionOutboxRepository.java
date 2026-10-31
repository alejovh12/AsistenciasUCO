package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Puerto de persistencia secundario para el outbox de notificaciones.
 */
public interface NotificacionOutboxRepository {

    void registrar(RegistroNotificacion registro);

    List<NotificacionReservada> reservar(int lote, Duration lease, String instancia);

    void marcarEntregada(UUID id, String instancia);

    void registrarFallo(
            UUID id,
            String instancia,
            String codigoError,
            String detalle,
            boolean reintentable,
            int maxIntentos,
            Duration base,
            Duration max
    );

    int depurar(int retencionDias, int lote);
}
