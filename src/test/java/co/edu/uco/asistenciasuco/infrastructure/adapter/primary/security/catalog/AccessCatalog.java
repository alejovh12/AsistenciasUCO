package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.catalog;

import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Definición compartida del control de acceso por endpoint ({@code infra/security-baseline/access-catalog.json}).
 * La lee {@code provision_asistencias.py} para registrar recursos y concesiones en el PDP y la verifican estas pruebas
 * contra Spring MVC (qué endpoints existen) y contra el {@code SecurityFilterChain} real (quién puede llamarlos).
 */
record AccessCatalog(
        int schemaVersion,
        String application,
        List<String> roles,
        List<Exempt> exempt,
        List<Profile> profiles,
        List<Entry> endpoints
) {

    static final Path LOCATION = Path.of("infra", "security-baseline", "access-catalog.json");

    record Exempt(String method, String path, String reason) {
    }

    record Profile(String name, List<String> roles, String description) {
    }

    record Entry(String method, String path, List<String> roles, String access, String ownership) {

        SpringMvcEndpointInventory.Endpoint endpoint() {
            return new SpringMvcEndpointInventory.Endpoint(method, path);
        }
    }

    static AccessCatalog load() {
        try {
            return JsonMapper.builder().build().readValue(Files.readString(LOCATION), AccessCatalog.class);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("No se pudo leer " + LOCATION, exception);
        }
    }
}
