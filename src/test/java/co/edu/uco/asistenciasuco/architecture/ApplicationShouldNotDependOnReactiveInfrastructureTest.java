package co.edu.uco.asistenciasuco.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationShouldNotDependOnReactiveInfrastructureTest {

    private static final Path APPLICATION_ROOT = Path.of("src/main/java/co/edu/uco/asistenciasuco/application");
    private static final List<String> FORBIDDEN_TOKENS = List.of(
            "reactor.core",
            "org.reactivestreams",
            "org.springframework.web.reactive",
            "Mono<",
            "Flux<",
            "Schedulers",
            "WebClient"
    );

    @Test
    void application_y_domain_no_importan_reactor_ni_webflux() throws IOException {
        final List<Path> violations;
        try (var files = Files.walk(APPLICATION_ROOT)) {
            violations = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(this::containsForbiddenToken)
                    .toList();
        }

        assertTrue(violations.isEmpty(), "Application/Domain con dependencias reactivas: " + violations);
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
