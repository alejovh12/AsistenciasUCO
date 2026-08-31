package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class KeycloakGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    private static final String CLIENT_NAME = "asistencias-api";
    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> convert(final Jwt jwt) {
        final Map<String, Object> resourceAccess = jwt.getClaim(RESOURCE_ACCESS_CLAIM);
        if (resourceAccess == null || !resourceAccess.containsKey(CLIENT_NAME)) {
            return Collections.emptyList();
        }

        final Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(CLIENT_NAME);
        if (clientAccess == null || !clientAccess.containsKey(ROLES_CLAIM)) {
            return Collections.emptyList();
        }

        final List<String> roles = (List<String>) clientAccess.get(ROLES_CLAIM);
        if (roles == null) {
            return Collections.emptyList();
        }

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
                .collect(Collectors.toList());
    }
}
