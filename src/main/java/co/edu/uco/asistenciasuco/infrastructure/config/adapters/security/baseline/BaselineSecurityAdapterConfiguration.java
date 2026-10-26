package co.edu.uco.asistenciasuco.infrastructure.config.adapters.security.baseline;

import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.baseline.BaselineJwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.contract.JwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.validation.AudienceValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.validation.InstitutionalIssuerValidator;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SecurityBaselineProviderProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** Composition Root del emisor OIDC central; no reutiliza el issuer legacy. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.adapters.security", name = "provider", havingValue = "baseline")
@EnableConfigurationProperties(SecurityBaselineProviderProperties.class)
public class BaselineSecurityAdapterConfiguration {

    @Bean
    public JwtClaimsExtractor jwtClaimsExtractor(
            final SecurityBaselineProviderProperties properties,
            final InstitutionalScopePort institutionalScope
    ) {
        return new BaselineJwtClaimsExtractor(
                properties.apiClientId(), properties.emailClaim(), properties.emailVerifiedClaim(), institutionalScope);
    }

    @Bean
    public JwtDecoder jwtDecoder(final SecurityBaselineProviderProperties properties) {
        final NimbusJwtDecoder decoder = JwtDecoders.fromIssuerLocation(properties.issuerUri());
        final OAuth2TokenValidator<Jwt> timestampValidator = new JwtTimestampValidator();
        final OAuth2TokenValidator<Jwt> issuerValidator = new InstitutionalIssuerValidator(properties.issuerUri());
        final OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(properties.expectedAudience());
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                timestampValidator, issuerValidator, audienceValidator));
        return decoder;
    }
}
