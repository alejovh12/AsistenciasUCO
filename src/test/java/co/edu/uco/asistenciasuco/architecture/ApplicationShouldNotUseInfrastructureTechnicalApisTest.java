package co.edu.uco.asistenciasuco.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationShouldNotUseInfrastructureTechnicalApisTest {

    private static final Path APPLICATION_ROOT = Path.of("src/main/java/co/edu/uco/asistenciasuco/application");
    private static final List<String> FORBIDDEN_TOKENS = List.of(
            "org.springframework.web",
            "org.springframework.http",
            "jakarta.servlet",
            "JdbcTemplate",
            "SimpleJdbcCall",
            "DataAccessException",
            "java.sql",
            "MDC",
            "CorrelationIdContext",
            "idCorrelacion",
            "IdCorrelacion",
            "HttpServletRequest",
            "ResponseEntity",
            "HttpStatus",
            "reactor.core",
            "org.reactivestreams",
            "org.springframework.web.reactive",
            "Mono",
            "Flux",
            "Schedulers",
            "WebClient"
    );

    @Test
    void application_no_importa_apis_tecnicas_de_infraestructura() throws IOException {
        final List<Path> violations;
        try (var files = Files.walk(APPLICATION_ROOT)) {
            violations = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(this::containsForbiddenToken)
                    .toList();
        }

        assertTrue(violations.isEmpty(), "Application con APIs tecnicas de infraestructura: " + violations);
    }

    private boolean containsForbiddenToken(final Path path) {
        try {
            final String content = Files.readString(path);
            return FORBIDDEN_TOKENS.stream().anyMatch(content::contains);
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible leer " + path, exception);
        }
    }
}
