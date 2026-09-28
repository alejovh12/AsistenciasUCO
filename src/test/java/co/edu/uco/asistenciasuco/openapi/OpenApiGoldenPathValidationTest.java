package co.edu.uco.asistenciasuco.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiGoldenPathValidationTest {

    private static final Path SPEC = Path.of("docs", "contracts", "openapi", "openapi-golden-path.yaml");
    private static final Path SHA = Path.of("docs", "contracts", "openapi", "openapi-golden-path.sha256");
    private static final Pattern LOCAL_REF = Pattern.compile(
            "\\$ref:\\s*['\"]?#/components/(schemas|responses|parameters|headers)/([A-Za-z0-9._-]+)['\"]?"
    );

    @Test
    void canonicalSpecIsValidOpenApi31AndResolvesEveryLocalReference() throws IOException {
        assertTrue(Files.isRegularFile(SPEC), "Falta el OpenAPI canónico: " + SPEC);

        final ParseOptions options = new ParseOptions();
        options.setResolve(true);
        final SwaggerParseResult result = new OpenAPIV3Parser().readLocation(
                SPEC.toAbsolutePath().toUri().toString(),
                null,
                options
        );

        assertNotNull(result.getOpenAPI(), () -> "Swagger Parser no pudo cargar el contrato: " + result.getMessages());
        assertTrue(
                result.getMessages() == null || result.getMessages().isEmpty(),
                () -> "OpenAPI inválido: " + result.getMessages()
        );

        final OpenAPI openAPI = result.getOpenAPI();
        assertEquals("3.1.2", openAPI.getOpenapi());
        assertNotNull(openAPI.getInfo());
        assertNotNull(openAPI.getServers());
        assertFalse(openAPI.getServers().isEmpty());
        assertNotNull(openAPI.getTags());
        assertNotNull(openAPI.getPaths());
        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getSecuritySchemes().get("bearerAuth"));

        assertLocalReferencesExist(Files.readString(SPEC, StandardCharsets.UTF_8), openAPI);
    }

    @Test
    void canonicalSha256MatchesExactYamlBytes() throws IOException, NoSuchAlgorithmException {
        assertTrue(Files.isRegularFile(SHA), "Falta el SHA canónico: " + SHA);
        final String expected = Files.readString(SHA, StandardCharsets.UTF_8).trim().split("\\s+")[0];
        final String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(SPEC)));
        assertEquals(expected, actual, "El YAML cambió sin regenerar openapi-golden-path.sha256");
    }

    private static void assertLocalReferencesExist(final String yaml, final OpenAPI openAPI) {
        final Map<String, ?> schemas = openAPI.getComponents().getSchemas();
        final Map<String, ?> responses = openAPI.getComponents().getResponses();
        final Map<String, ?> parameters = openAPI.getComponents().getParameters();
        final Map<String, ?> headers = openAPI.getComponents().getHeaders();
        final Map<String, Map<String, ?>> components = Map.of(
                "schemas", schemas,
                "responses", responses,
                "parameters", parameters,
                "headers", headers
        );

        final Matcher matcher = LOCAL_REF.matcher(yaml);
        int count = 0;
        while (matcher.find()) {
            count++;
            final String section = matcher.group(1);
            final String name = matcher.group(2);
            assertTrue(
                    components.get(section).containsKey(name),
                    () -> "Broken $ref: #/components/" + section + "/" + name
            );
        }
        assertTrue(count > 0, "El contrato debe reutilizar components mediante $ref");
    }
}
