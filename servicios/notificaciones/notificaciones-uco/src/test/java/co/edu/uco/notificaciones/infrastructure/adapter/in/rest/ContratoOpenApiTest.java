package co.edu.uco.notificaciones.infrastructure.adapter.in.rest;

import co.edu.uco.notificaciones.application.port.in.NotificacionesUseCase;
import co.edu.uco.notificaciones.application.port.in.PlantillasUseCase;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Conformidad con el contrato congelado ({@code ../contrato}): las operaciones de /api/v1 del servicio son exactamente las del
 * OpenAPI y el .sha256 corresponde al YAML. Si el modulo se compila fuera de la carpeta del componente (p. ej. en la imagen
 * Docker) la prueba se omite.
 */
@WebMvcTest(controllers = {NotificacionesController.class, PlantillasController.class, CanalController.class},
        properties = "notificaciones.api-key=clave-de-prueba")
class ContratoOpenApiTest {

    private static final Path YAML = Path.of("..", "contrato", "openapi-notificaciones.yaml");
    private static final Path HASH = Path.of("..", "contrato", "openapi-notificaciones.sha256");

    @Autowired
    private RequestMappingHandlerMapping mapeo;
    @MockitoBean
    private NotificacionesUseCase notificaciones;
    @MockitoBean
    private PlantillasUseCase plantillas;

    @Test
    void lasOperacionesDelServicioSonLasDelContrato() throws IOException {
        Assumptions.assumeTrue(Files.exists(YAML), "contrato no disponible en este contexto de compilacion");
        final Map<String, Object> documento = new Yaml().load(Files.readString(YAML, StandardCharsets.UTF_8));
        @SuppressWarnings("unchecked")
        final Map<String, Map<String, Object>> rutas = (Map<String, Map<String, Object>>) documento.get("paths");
        final Set<String> enContrato = new TreeSet<>();
        rutas.forEach((ruta, operaciones) -> {
            if (ruta.startsWith("/api/v1")) {
                operaciones.keySet().stream().filter(k -> Set.of("get", "post", "put", "delete", "patch").contains(k))
                        .forEach(metodo -> enContrato.add(metodo.toUpperCase() + " " + ruta));
            }
        });
        final Set<String> enServicio = new TreeSet<>();
        for (final RequestMappingInfo info : mapeo.getHandlerMethods().keySet()) {
            for (final RequestMethod metodo : info.getMethodsCondition().getMethods()) {
                for (final String patron : info.getPathPatternsCondition().getPatternValues()) {
                    if (patron.startsWith("/api/v1")) {
                        enServicio.add(metodo.name() + " " + patron);
                    }
                }
            }
        }
        assertEquals(enContrato, enServicio);
        assertEquals(9, enServicio.size());
    }

    @Test
    void elHashCoincideConElYaml() throws Exception {
        Assumptions.assumeTrue(Files.exists(YAML) && Files.exists(HASH), "contrato no disponible en este contexto de compilacion");
        final String esperado = Files.readString(HASH, StandardCharsets.UTF_8).trim().split("\\s+")[0];
        final String real = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(YAML)));
        assertEquals(esperado, real, "El contrato cambio: actualice openapi-notificaciones.sha256 (WORM, con addendum)");
        assertTrue(Files.readString(HASH, StandardCharsets.UTF_8).trim().endsWith("openapi-notificaciones.yaml"));
    }
}
