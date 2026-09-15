package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt;

import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.contract.JwtClaimsExtractor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Convierte un {@link Jwt} en un {@link JwtAuthenticationToken} cuyo principal es siempre el
 * UUID institucional del usuario ({@code idUsuario}), nunca {@code sub}, {@code email} ni
 * {@code preferred_username}.
 *
 * <p>Depende únicamente de {@link JwtClaimsExtractor} (una SPI de infraestructura neutral), no
 * de ningún proveedor concreto. La resolución de qué proveedor implementa esa SPI ocurre en el
 * Composition Root de seguridad.</p>
 *
 * <p>Este es el ÚNICO lugar de todo el runtime de seguridad donde existe la convención
 * {@code ROLE_} de Spring Security: {@link InstitutionalRole} es puro y no la conoce.</p>
 */
public final class InstitutionalJwtAuthenticationConverter implements Converter<Jwt, JwtAuthenticationToken> {

    private static final String SPRING_AUTHORITY_PREFIX = "ROLE_";

    private final JwtClaimsExtractor jwtClaimsExtractor;

    public InstitutionalJwtAuthenticationConverter(final JwtClaimsExtractor jwtClaimsExtractor) {
        this.jwtClaimsExtractor = Objects.requireNonNull(jwtClaimsExtractor, "JwtClaimsExtractor es obligatorio.");
    }

    @Override
    public JwtAuthenticationToken convert(final Jwt jwt) {
        final UUID idUsuario = jwtClaimsExtractor.requireUserId(jwt);

        final Collection<GrantedAuthority> authorities = jwtClaimsExtractor.extractRoles(jwt).stream()
                .map(role -> SPRING_AUTHORITY_PREFIX + role.name())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());

        return new JwtAuthenticationToken(jwt, authorities, idUsuario.toString());
    }
}
