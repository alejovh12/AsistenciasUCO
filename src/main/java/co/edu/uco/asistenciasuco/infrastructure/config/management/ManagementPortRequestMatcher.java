package co.edu.uco.asistenciasuco.infrastructure.config.management;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Objects;

/**
 * VAL H2 (OBS-08): reconoce las peticiones que llegaron por el puerto de gestion de Actuator.
 *
 * <p>Usa los puertos reales que Spring Boot publica al arrancar cada servidor ({@code local.management.port} y
 * {@code local.server.port}); asi funciona tambien con puertos aleatorios en pruebas. Solo responde {@code true} cuando el
 * puerto de gestion existe, es distinto del de la API y la peticion entro por el. Si los puertos coinciden (configuracion que
 * {@link ManagementServerPortGuard} ya rechaza) o el servidor de gestion no arranco, nunca coincide: el puerto de la API sigue
 * con su cadena de seguridad normal.</p>
 */
public final class ManagementPortRequestMatcher implements RequestMatcher {

    static final String LOCAL_MANAGEMENT_PORT = "local.management.port";
    static final String LOCAL_SERVER_PORT = "local.server.port";

    private final Environment environment;

    public ManagementPortRequestMatcher(final Environment environment) {
        this.environment = Objects.requireNonNull(environment, "El Environment es obligatorio.");
    }

    @Override
    public boolean matches(final HttpServletRequest request) {
        final Integer puertoGestion = environment.getProperty(LOCAL_MANAGEMENT_PORT, Integer.class);
        if (puertoGestion == null || puertoGestion <= 0) {
            return false;
        }
        final Integer puertoApi = environment.getProperty(LOCAL_SERVER_PORT, Integer.class);
        return !puertoGestion.equals(puertoApi) && request.getLocalPort() == puertoGestion;
    }

    @Override
    public String toString() {
        return "ManagementPortRequestMatcher";
    }
}
