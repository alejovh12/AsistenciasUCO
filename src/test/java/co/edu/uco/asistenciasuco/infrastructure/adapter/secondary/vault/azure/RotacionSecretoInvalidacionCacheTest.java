package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.vault.azure;

import co.edu.uco.asistenciasuco.application.features.admin.procesareventoazure.usecase.impl.ProcesarEventoAzureUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.composite.CompositeCatalogInvalidationAdapter;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SEC-10 (VAL-oleada3-baul-secretos): una rotacion en el baul publica un evento de Event Grid; el evento invalida la
 * cache del adaptador y la siguiente lectura devuelve la version nueva, sin esperar el TTL de 5 minutos.
 *
 * <p>Recorre la cadena real del webhook a partir del caso de uso: {@link ProcesarEventoAzureUseCaseImpl} ->
 * {@link CompositeCatalogInvalidationAdapter} -> {@link AzureKeyVaultAdapter}. Solo el {@link SecretClient} es un
 * mock (sin Azure real). Valores ficticios de prueba.</p>
 */
class RotacionSecretoInvalidacionCacheTest {

    private static final String SECRETO = "asistencias-eventgrid-webhook-token";

    private SecretClient secretClient;
    private AzureKeyVaultAdapter baul;
    private ProcesarEventoAzureUseCaseImpl procesarEvento;
    private final List<RealtimeEvent> eventosRealtime = new ArrayList<>();

    @BeforeEach
    void setUp() {
        secretClient = Mockito.mock(SecretClient.class);
        // TTL largo: si la segunda lectura trae la version nueva, es por la invalidacion y no por expiracion.
        baul = new AzureKeyVaultAdapter(secretClient,
                Caffeine.newBuilder().maximumSize(50).expireAfterWrite(Duration.ofHours(1)).build());
        final CompositeCatalogInvalidationAdapter invalidacion = new CompositeCatalogInvalidationAdapter(
                Mockito.mock(ParameterCatalogPort.class), Mockito.mock(MessageCatalogPort.class), baul);
        procesarEvento = new ProcesarEventoAzureUseCaseImpl(invalidacion, eventosRealtime::add);
    }

    private static KeyVaultSecret version(final String valor) {
        return new KeyVaultSecret(SECRETO, valor);
    }

    @Test
    void evento_de_nueva_version_invalida_la_cache_y_la_siguiente_lectura_trae_la_version_nueva() {
        final KeyVaultSecret v1 = version("valor-version-1");
        final KeyVaultSecret v2 = version("valor-version-2");
        when(secretClient.getSecret(SECRETO)).thenReturn(v1, v2);

        assertEquals("valor-version-1", baul.getSecret(SECRETO).orElseThrow());
        // Antes del evento la cache sigue sirviendo la version vigente sin volver al baul.
        assertEquals("valor-version-1", baul.getSecret(SECRETO).orElseThrow());
        verify(secretClient, times(1)).getSecret(SECRETO);

        procesarEvento.procesarEvento("Microsoft.KeyVault.SecretNewVersionCreated",
                "/subscriptions/x/resourceGroups/x/providers/Microsoft.KeyVault/vaults/x/secrets/" + SECRETO,
                Map.of("ObjectName", SECRETO, "ObjectType", "Secret", "Version", "v2"));

        assertEquals("valor-version-2", baul.getSecret(SECRETO).orElseThrow());
        verify(secretClient, times(2)).getSecret(SECRETO);
        assertTrue(eventosRealtime.isEmpty(), "un cambio de secreto nunca se difunde por realtime");
    }

    @Test
    void evento_de_otro_secreto_no_invalida_el_valor_vigente() {
        when(secretClient.getSecret(SECRETO)).thenReturn(version("valor-version-1"), version("valor-version-2"));
        baul.getSecret(SECRETO);

        procesarEvento.procesarEvento("Microsoft.KeyVault.SecretNewVersionCreated", "",
                Map.of("ObjectName", "otro-secreto"));

        assertEquals("valor-version-1", baul.getSecret(SECRETO).orElseThrow());
        verify(secretClient, times(1)).getSecret(SECRETO);
    }

    @Test
    void evento_de_expiracion_tambien_obliga_a_releer_el_secreto() {
        when(secretClient.getSecret(SECRETO)).thenReturn(version("valor-version-1"), version("valor-version-2"));
        baul.getSecret(SECRETO);

        procesarEvento.procesarEvento("Microsoft.KeyVault.SecretExpired", SECRETO, Map.of());

        assertEquals("valor-version-2", baul.getSecret(SECRETO).orElseThrow());
    }
}
