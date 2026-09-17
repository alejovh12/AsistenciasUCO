package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.validation;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Validador de emisor (iss) institucional. Valida que el claim {@code iss} del JWT
 * coincida con el emisor configurado, admitiendo equivalencia transparente entre
 * {@code localhost} y {@code 127.0.0.1} para evitar fallos de autenticacion en entornos
 * de desarrollo local cuando el cliente web accede por diferente nombre de host.
 */
public final class InstitutionalIssuerValidator implements OAuth2TokenValidator<Jwt> {

    private final String expectedIssuer;
    private final Set<String> acceptedIssuers;

    public InstitutionalIssuerValidator(final String expectedIssuer) {
        if (expectedIssuer == null || expectedIssuer.isBlank()) {
            throw new IllegalArgumentException("expectedIssuer es obligatorio.");
        }
        this.expectedIssuer = expectedIssuer.trim();
        final Set<String> issuers = new LinkedHashSet<>();
        issuers.add(this.expectedIssuer);

        if (this.expectedIssuer.contains("localhost")) {
            issuers.add(this.expectedIssuer.replace("localhost", "127.0.0.1"));
        } else if (this.expectedIssuer.contains("127.0.0.1")) {
            issuers.add(this.expectedIssuer.replace("127.0.0.1", "localhost"));
        }
        this.acceptedIssuers = Collections.unmodifiableSet(issuers);
    }

    @Override
    public OAuth2TokenValidatorResult validate(final Jwt token) {
        final URL issuer = token.getIssuer();
        if (issuer == null || !acceptedIssuers.contains(issuer.toString())) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    OAuth2ErrorCodes.INVALID_TOKEN,
                    "El emisor del token (iss) no es valido. Se esperaba uno de: " + acceptedIssuers,
                    "https://tools.ietf.org/html/rfc6750#section-3.1"
            ));
        }
        return OAuth2TokenValidatorResult.success();
    }

    public String expectedIssuer() {
        return expectedIssuer;
    }

    public Set<String> acceptedIssuers() {
        return acceptedIssuers;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof InstitutionalIssuerValidator otherValidator
                && Objects.equals(expectedIssuer, otherValidator.expectedIssuer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expectedIssuer);
    }
}
