package co.edu.uco.notificaciones.application.port.out;

import co.edu.uco.notificaciones.application.model.FalloReciente;
import co.edu.uco.notificaciones.application.model.FiltroNotificaciones;
import co.edu.uco.notificaciones.application.model.PaginaNotificaciones;
import co.edu.uco.notificaciones.application.model.ResultadoRegistro;
import co.edu.uco.notificaciones.application.model.ResultadoReintento;
import co.edu.uco.notificaciones.domain.Notificacion;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: cola persistente de notificaciones. */
public interface NotificacionRepositoryPort {

    /** Inserta la notificacion; si ya existe la clave (aplicacion, idempotencyKey) devuelve la existente. */
    ResultadoRegistro registrarSiNoExiste(Notificacion nueva);

    Optional<Notificacion> buscarPorId(UUID id);

    /** Reserva un lote vigente (PENDIENTE o FALLIDA_REINTENTABLE vencidas, o RESERVADA con lease vencido) y suma un intento. */
    List<Notificacion> reservar(int lote, Duration lease, String instancia, Instant ahora);

    /** @return false si la reserva ya no es de esta instancia. */
    boolean marcarEntregada(UUID id, String instancia, Instant ahora);

    /** Registra el fallo y calcula el backoff MIN(base por 2 elevado a intentos-1, max); false si perdio la reserva. */
    boolean registrarFallo(UUID id, String instancia, String codigoError, String detalle, boolean reintentable,
                           int maxIntentos, Duration backoffBase, Duration backoffMax, Instant ahora);

    /** Devuelve a PENDIENTE las reservas que no se alcanzaron a procesar, sin contar el intento. */
    int liberar(List<UUID> ids, String instancia, Instant ahora);

    ResultadoReintento reencolar(UUID id, Instant ahora);

    PaginaNotificaciones consultar(FiltroNotificaciones filtro, int pagina, int tamano);

    int depurar(Instant limite, int lote);

    Optional<Instant> ultimaEntrega();

    Optional<FalloReciente> ultimoFallo();
}
