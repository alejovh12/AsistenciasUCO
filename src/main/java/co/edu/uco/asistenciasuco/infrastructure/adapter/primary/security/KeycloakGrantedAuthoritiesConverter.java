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

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> convert(final Jwt jwt) {
        final java.util.Set<String> allRoles = new java.util.HashSet<>();

        // 1. Extraer realm_access.roles
        final Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
        if (realmAccess != null && realmAccess.containsKey(ROLES_CLAIM)) {
            final List<String> realmRoles = (List<String>) realmAccess.get(ROLES_CLAIM);
            if (realmRoles != null) {
                allRoles.addAll(realmRoles);
            }
        }

        // 2. Extraer resource_access.*.roles (asistencias-api, asistencias-uco-frontend, etc.)
        final Map<String, Object> resourceAccess = jwt.getClaim(RESOURCE_ACCESS_CLAIM);
        if (resourceAccess != null) {
            for (final Object clientObj : resourceAccess.values()) {
                if (clientObj instanceof Map<?, ?> clientMap && clientMap.containsKey(ROLES_CLAIM)) {
                    final List<String> clientRoles = (List<String>) clientMap.get(ROLES_CLAIM);
                    if (clientRoles != null) {
                        allRoles.addAll(clientRoles);
                    }
                }
            }
        }

        return allRoles.stream()
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
                .collect(Collectors.toList());
    }
}
