package co.edu.uco.asistenciasuco.infrastructure.config.adapters.vault.runtime;

import co.edu.uco.asistenciasuco.application.secondaryports.vault.SecretVaultPort;
import co.edu.uco.asistenciasuco.application.secondaryports.vault.SecretVaultPort.SecretVaultException;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Fuente de propiedades con los secretos de ejecucion leidos del baul al arrancar (SEC-02).
 *
 * <p>Solo contiene los secretos que existen en el baul. Un secreto ausente no se agrega y la propiedad sigue
 * resolviendose con la fuente siguiente (variables de entorno o secretos del orquestador): ese es el fallback
 * documentado. Un fallo tecnico del baul (red, identidad, permisos) detiene el arranque: nunca se sirve un valor
 * inventado. Es una foto del arranque: las propiedades ya enlazadas (pool de BD, clientes HTTP) solo toman una
 * version nueva al reiniciar la instancia (ver {@code docs/security/SECRETS_ROTATION_RUNBOOK.md}).</p>
 */
public final class VaultSecretPropertySource extends MapPropertySource {

    /** Nombre de la fuente en el {@code Environment}. */
    public static final String NOMBRE = "asistenciasVaultRuntimeSecrets";

    private VaultSecretPropertySource(final Map<String, Object> valores) {
        super(NOMBRE, valores);
    }

    /**
     * Lee del puerto cada secreto del catalogo. Nunca registra ni expone valores en mensajes.
     *
     * @param baul     puerto del baul activo.
     * @param catalogo propiedad Spring -> nombre del secreto.
     * @return fuente con las propiedades resueltas (puede quedar vacia).
     * @throws IllegalStateException si el baul falla tecnicamente; el mensaje nombra la propiedad y el secreto.
     */
    public static VaultSecretPropertySource cargar(final SecretVaultPort baul, final Map<String, String> catalogo) {
        Objects.requireNonNull(baul, "El puerto del baul es obligatorio.");
        Objects.requireNonNull(catalogo, "El catalogo de secretos es obligatorio.");
        final Map<String, Object> valores = new LinkedHashMap<>();
        catalogo.forEach((propiedad, secreto) -> leer(baul, propiedad, secreto)
                .ifPresent(valor -> valores.put(propiedad, valor)));
        return new VaultSecretPropertySource(valores);
    }

    private static Optional<String> leer(final SecretVaultPort baul, final String propiedad, final String secreto) {
        try {
            return baul.getSecret(secreto).filter(valor -> !valor.isBlank());
        } catch (final SecretVaultException e) {
            throw new IllegalStateException("No se pudo leer del baul el secreto '" + secreto + "' de la propiedad '"
                    + propiedad + "'. Revise AZURE_KEYVAULT_ENDPOINT, la identidad de DefaultAzureCredential y su "
                    + "permiso de lectura de secretos, o desactive la carga con APP_SECRETS_RUNTIME_FROM_VAULT_ENABLED=false.",
                    e);
        }
    }
}
