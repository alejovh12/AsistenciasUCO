package co.edu.uco.asistenciasuco.infrastructure.config.management;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * CULM-001 D.9-a (F-TRX-020): el puerto de gestion (Actuator/Prometheus, {@code MANAGEMENT_SERVER_PORT}, defecto 8090, solo
 * loopback) debe ser distinto del puerto de la API. Si coinciden, Prometheus volveria a quedar en el puerto publico: el arranque
 * falla con un mensaje claro en lugar de exponer las metricas.
 */
@Component
public class ManagementServerPortGuard {

    public ManagementServerPortGuard(
            @Value("${server.port:8080}") final int serverPort,
            @Value("${management.server.port:-1}") final int managementPort
    ) {
        if (managementPort > 0 && managementPort == serverPort) {
            throw new IllegalStateException("MANAGEMENT_SERVER_PORT (" + managementPort
                    + ") coincide con server.port: las metricas de Actuator deben salir por un puerto de gestion distinto.");
        }
    }
}
