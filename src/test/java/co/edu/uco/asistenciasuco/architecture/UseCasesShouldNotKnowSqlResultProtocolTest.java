package co.edu.uco.asistenciasuco.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UseCasesShouldNotKnowSqlResultProtocolTest {

    private static final Path USECASE_ROOT = Path.of("src/main/java/co/edu/uco/asistenciasuco/application/features");
    private static final List<String> FORBIDDEN_TOKENS = List.of(
            "DbExceptionTranslator",
            "estadoResultado",
            "mensajeUsuarioResultado",
            "mensajeTecnicoResultado",
            "codigoResultado",
            "SimpleJdbcCall",
            "JdbcTemplate"
    );

    @Test
    void usecase_impl_no_conoce_protocolo_de_resultado_sql() throws IOException {
        final List<Path> violations;
        try (var files = Files.walk(USECASE_ROOT)) {
            violations = files
                    .filter(path -> path.toString().endsWith("UseCaseImpl.java"))
                    .filter(this::containsForbiddenToken)
                    .toList();
        }

        assertTrue(violations.isEmpty(), "UseCaseImpl con protocolo SQL: " + violations);
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
