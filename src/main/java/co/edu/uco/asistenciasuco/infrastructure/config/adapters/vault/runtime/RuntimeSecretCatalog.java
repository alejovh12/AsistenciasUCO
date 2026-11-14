package co.edu.uco.asistenciasuco.infrastructure.config.adapters.vault.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Inventario de secretos de ejecucion que se resuelven desde el baul cuando
 * {@code app.adapters.vault.provider=azure_keyvault} (SEC-02, VAL-oleada3-baul-secretos).
 *
 * <p>Relaciona cada propiedad Spring con el nombre del secreto en Azure Key Vault (solo letras, digitos y guiones).
 * Son nombres, nunca valores. La tabla humana con duenos y rotacion esta en
 * {@code docs/security/SECRETS_INVENTORY.md}; ambas deben cambiar juntas (lo comprueba
 * {@code RuntimeSecretCatalogTest}).</p>
 */
public final class RuntimeSecretCatalog {

    /** Propiedad Spring -> nombre del secreto en el baul. Orden estable para logs y documentacion. */
    public static final Map<String, String> PROPIEDAD_A_SECRETO;

    static {
        final Map<String, String> catalogo = new LinkedHashMap<>();
        catalogo.put("spring.datasource.username", "asistencias-db-username");
        catalogo.put("spring.datasource.password", "asistencias-db-password");
        catalogo.put("app.providers.keycloak-identity.admin-client-secret", "asistencias-keycloak-admin-client-secret");
        catalogo.put("app.providers.security-baseline-identity.admin-client-secret",
                "asistencias-baseline-identity-admin-client-secret");
        catalogo.put("app.providers.security-baseline-pdp-admin.client-secret", "asistencias-pdp-admin-client-secret");
        catalogo.put("app.providers.security-baseline-pep.application-credential",
                "asistencias-pep-application-credential");
        catalogo.put("app.providers.minio.access-key", "asistencias-minio-access-key");
        catalogo.put("app.providers.minio.secret-key", "asistencias-minio-secret-key");
        catalogo.put("app.providers.smtp-notification.username", "asistencias-smtp-username");
        catalogo.put("app.providers.smtp-notification.password", "asistencias-smtp-password");
        catalogo.put("app.security.azure-events.webhook-token", "asistencias-eventgrid-webhook-token");
        catalogo.put("app.providers.catalogo-parametros.secreto", "asistencias-catalogo-parametros-secreto");
        PROPIEDAD_A_SECRETO = Collections.unmodifiableMap(catalogo);
    }

    private RuntimeSecretCatalog() {
    }
}
