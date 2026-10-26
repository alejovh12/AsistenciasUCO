package co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters;

import java.util.Objects;

/**
 * El selector de seguridad ({@code app.adapters.security.provider}) y el de aprovisionamiento de identidades
 * ({@code app.adapters.identity.provider}) deben apuntar al mismo proveedor: en modo baseline las cuentas nuevas
 * nunca se crean en el Keycloak legacy (8081) y en modo legacy nunca en el realm central. Un valor desconocido de
 * cualquiera de los dos ya falla el binding (enum cerrado); esta comprobación impide además la combinación mixta.
 * Se ejecuta en el arranque: no hay degradación silenciosa a otro proveedor.
 */
public final class SecurityProviderCoherence {

    public enum Mode {
        LEGACY,
        BASELINE
    }

    private SecurityProviderCoherence() {
    }

    public static Mode security(final SecurityAdapterProperties.Provider provider) {
        return switch (Objects.requireNonNull(provider, "El proveedor de seguridad es obligatorio.")) {
            case KEYCLOAK, LEGACY -> Mode.LEGACY;
            case BASELINE -> Mode.BASELINE;
        };
    }

    public static Mode identity(final IdentityAdapterProperties.Provider provider) {
        return switch (Objects.requireNonNull(provider, "El proveedor de identidad es obligatorio.")) {
            case KEYCLOAK, LEGACY -> Mode.LEGACY;
            case BASELINE -> Mode.BASELINE;
        };
    }

    /** @throws IllegalStateException si los selectores no son coherentes. */
    public static Mode require(
            final SecurityAdapterProperties security,
            final IdentityAdapterProperties identity
    ) {
        final Mode securityMode = security(security.provider());
        final Mode identityMode = identity(identity.provider());
        if (securityMode != identityMode) {
            throw new IllegalStateException(
                    "Proveedores de seguridad incoherentes: app.adapters.security.provider=" + security.provider()
                            + " pero app.adapters.identity.provider=" + identity.provider()
                            + ". Ambos deben seleccionar el mismo proveedor (legacy o baseline); defina solo "
                            + "APP_ADAPTERS_SECURITY_PROVIDER o haga coincidir APP_ADAPTERS_IDENTITY_PROVIDER.");
        }
        return securityMode;
    }
}
