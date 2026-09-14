package co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

/**
 * Selección del publicador de auditoría.
 *
 * <p>El wiring real actual solo tiene una implementación de {@code AuditEventPublisher}
 * ({@code LoggingAuditEventPublisher}), que registra en log y, si el repositorio SQL Server
 * está disponible, también persiste el evento. No existen hoy dos publicadores independientes
 * seleccionables (uno solo SQL Server y otro solo logging), por lo que el único valor soportado
 * es {@code LOGGING}.</p>
 */
@ConfigurationProperties(prefix = "app.adapters.audit")
public record AuditAdapterProperties(Provider provider) {

    public AuditAdapterProperties {
        Objects.requireNonNull(provider, "app.adapters.audit.provider es obligatorio.");
    }

    public enum Provider {
        LOGGING
    }
}
