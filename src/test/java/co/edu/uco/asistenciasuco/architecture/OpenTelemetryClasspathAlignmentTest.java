package co.edu.uco.asistenciasuco.architecture;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.internal.SdkConfigProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * INT-003: el SDK de OpenTelemetry (gestionado por Spring Boot) y {@code opentelemetry-api-incubator} (lo trae
 * {@code pulsar-client}) deben ser de la misma version. Con 1.62.0 y 1.56.0-alpha el Backend no arrancaba
 * ({@code SdkConfigProvider} llama a {@code DeclarativeConfigProperties.get(String)}, que la 1.56 no tiene) y las pruebas
 * unitarias no lo veian porque no construyen el SDK real.
 */
class OpenTelemetryClasspathAlignmentTest {

    @Test
    void el_incubator_tiene_la_api_que_usa_el_sdk() {
        assertDoesNotThrow(() -> DeclarativeConfigProperties.class.getMethod("get", String.class));
    }

    @Test
    void sdk_e_incubator_son_de_la_misma_version() {
        final String sdk = version(SdkConfigProvider.class);
        final String incubator = version(DeclarativeConfigProperties.class);
        assertEquals(sdk, incubator.replace("-alpha", ""), () -> "opentelemetry-sdk " + sdk + " vs api-incubator " + incubator);
    }

    /** Version del jar a partir de su nombre ({@code artefacto-X.Y.Z[-alpha].jar}); el manifiesto no la declara. */
    private static String version(final Class<?> tipo) {
        final String ruta = tipo.getProtectionDomain().getCodeSource().getLocation().getPath();
        final String jar = ruta.substring(ruta.lastIndexOf('/') + 1);
        final java.util.regex.Matcher m = java.util.regex.Pattern.compile("-(\\d+\\.\\d+\\.\\d+(?:-alpha)?)\\.jar$").matcher(jar);
        if (!m.find()) {
            throw new IllegalStateException("No se pudo leer la version de " + jar);
        }
        return m.group(1);
    }
}
