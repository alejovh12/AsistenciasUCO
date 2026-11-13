package co.edu.uco.asistenciasuco.infrastructure.config.adapters.vault.runtime;

import co.edu.uco.asistenciasuco.application.secondaryports.vault.SecretVaultPort;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.vault.azure.AzureKeyVaultClientFactory;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.AzureKeyVaultProviderProperties;
import org.apache.commons.logging.Log;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Composition Root de los secretos de ejecucion (SEC-02, VAL-oleada3-baul-secretos).
 *
 * <p>Con {@code app.adapters.vault.provider=azure_keyvault} lee del baul los secretos de
 * {@link RuntimeSecretCatalog} (base de datos, Keycloak, PDP/PEP, MinIO, SMTP y webhook de Event Grid) y los antepone
 * a las demas fuentes. El acceso pasa por {@link SecretVaultPort}; el SDK de Azure queda en
 * {@link AzureKeyVaultClientFactory}.</p>
 *
 * <ul>
 *   <li>Proveedor distinto de {@code azure_keyvault} (local_env en local, test e integracion): no hace nada; los
 *   secretos llegan por variables de entorno o secretos del orquestador, como antes.</li>
 *   <li>Proveedor Azure sin endpoint: falla el arranque con un mensaje claro (SEC-12).</li>
 *   <li>Secreto ausente en el baul: se usa la variable de entorno equivalente (fallback documentado).</li>
 *   <li>Fallo tecnico del baul: falla el arranque (fail-closed).</li>
 *   <li>{@code app.secrets.runtime-from-vault.enabled=false}: desactiva solo esta carga (el puerto sigue activo).</li>
 * </ul>
 *
 * <p>Corre despues de cargar {@code application.yml}. Se registra en {@code META-INF/spring.factories}.</p>
 */
public class VaultRuntimeSecretsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    public static final String PROPIEDAD_PROVIDER = "app.adapters.vault.provider";
    public static final String PROVIDER_AZURE = "azure_keyvault";
    public static final String PROPIEDAD_HABILITADO = "app.secrets.runtime-from-vault.enabled";
    public static final String PROPIEDAD_ENDPOINT = "app.providers.azure-keyvault.endpoint";

    private final Log log;
    private final Function<String, SecretVaultPort> fabricaBaul;
    private final Map<String, String> catalogo;

    public VaultRuntimeSecretsEnvironmentPostProcessor(final DeferredLogFactory logFactory) {
        this(logFactory, AzureKeyVaultClientFactory::crearAdaptador, RuntimeSecretCatalog.PROPIEDAD_A_SECRETO);
    }

    VaultRuntimeSecretsEnvironmentPostProcessor(
            final DeferredLogFactory logFactory,
            final Function<String, SecretVaultPort> fabricaBaul,
            final Map<String, String> catalogo
    ) {
        this.log = Objects.requireNonNull(logFactory, "logFactory es obligatorio.")
                .getLog(VaultRuntimeSecretsEnvironmentPostProcessor.class);
        this.fabricaBaul = Objects.requireNonNull(fabricaBaul, "La fabrica del baul es obligatoria.");
        this.catalogo = Objects.requireNonNull(catalogo, "El catalogo de secretos es obligatorio.");
    }

    @Override
    public void postProcessEnvironment(final ConfigurableEnvironment environment, final SpringApplication application) {
        final String provider = environment.getProperty(PROPIEDAD_PROVIDER, "").trim();
        if (!PROVIDER_AZURE.equalsIgnoreCase(provider)) {
            return;
        }
        if (!environment.getProperty(PROPIEDAD_HABILITADO, Boolean.class, Boolean.TRUE)) {
            log.info("Carga de secretos de ejecucion desde el baul desactivada (" + PROPIEDAD_HABILITADO + "=false).");
            return;
        }
        final String endpoint = environment.getProperty(PROPIEDAD_ENDPOINT, "").trim();
        if (endpoint.isEmpty()) {
            throw new IllegalStateException(AzureKeyVaultProviderProperties.MENSAJE_ENDPOINT_OBLIGATORIO);
        }

        final VaultSecretPropertySource fuente = VaultSecretPropertySource.cargar(fabricaBaul.apply(endpoint), catalogo);
        environment.getPropertySources().addFirst(fuente);
        log.info("Secretos de ejecucion: " + fuente.getPropertyNames().length + " de " + catalogo.size()
                + " propiedades resueltas desde el baul; el resto usa variables de entorno o secretos del orquestador. "
                + "Resueltas: " + String.join(", ", fuente.getPropertyNames()));
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }
}
