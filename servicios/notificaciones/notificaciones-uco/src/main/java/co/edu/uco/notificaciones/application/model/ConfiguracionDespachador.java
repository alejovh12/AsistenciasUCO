package co.edu.uco.notificaciones.application.model;

import java.time.Duration;

/** Parametros del despachador de la cola (lote, lease, reintentos con backoff y retencion). */
public record ConfiguracionDespachador(
        boolean habilitado,
        int batchSize,
        Duration lease,
        Duration sweeperInterval,
        int maxAttempts,
        Duration backoffBase,
        Duration backoffMax,
        int retentionDays,
        Duration retentionInterval,
        String instanceId
) {
}
