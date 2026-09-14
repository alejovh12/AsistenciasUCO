package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.spi;

import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Set;
import java.util.UUID;

/**
 * SPI de infraestructura que aísla la forma en que un proveedor OIDC concreto estructura
 * sus claims. Conoce {@link Jwt} porque eso es una preocupación de infraestructura (Spring
 * Security), pero no conoce ningún proveedor concreto (Keycloak, Auth0...) ni lógica de
 * negocio de Application.
 *
 * <p>La única implementación seleccionable hoy es {@code KeycloakJwtClaimsAdapter}, registrada
 * por el Composition Root de seguridad según {@code app.adapters.security.provider}.</p>
 */
public interface JwtClaimsAdapter {

    /**
     * Resuelve el identificador institucional del usuario autenticado.
     *
     * @throws org.springframework.security.oauth2.core.OAuth2AuthenticationException si el
     *         claim requerido está ausente, vacío o no es un UUID válido.
     */
    UUID requireUserId(Jwt jwt);

    /**
     * Extrae los roles institucionales reconocidos del token. Roles ajenos a
     * {@link InstitutionalRole} (de otros clients, o roles técnicos como
     * {@code offline_access}) se ignoran silenciosamente.
     */
    Set<InstitutionalRole> extractRoles(Jwt jwt);
}
