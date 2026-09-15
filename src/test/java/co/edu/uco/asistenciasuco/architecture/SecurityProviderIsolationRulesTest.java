package co.edu.uco.asistenciasuco.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Guardrails del desacople de runtime security respecto a Keycloak, descrito en
 * docs/security/runtime-security-provider-architecture.md.
 *
 * <p>Objetivo: {@code SecurityConfig} → Keycloak = 0; Application no conoce Keycloak, Jwt ni
 * Spring Security; los Controllers no conocen {@code JwtClaimsExtractor} ni Keycloak
 * directamente (solo {@code AuthenticatedUserResolver}, InputPorts y modelos HTTP).</p>
 */
class SecurityProviderIsolationRulesTest {

    private static final String BASE_PACKAGE = "co.edu.uco.asistenciasuco";
    private static final String SECURITY_CONFIG_CLASS = "co.edu.uco.asistenciasuco.infrastructure.config.security.SecurityConfig";
    private static final String CONTROLLER_PACKAGE = "..infrastructure.adapter.primary.controller..";

    // 41: SecurityConfig -> Keycloak = 0
    @Test
    void security_config_no_depende_de_paquetes_keycloak() {
        noClasses()
                .that().haveFullyQualifiedName(SECURITY_CONFIG_CLASS)
                .should().dependOnClassesThat().resideInAnyPackage("..keycloak..")
                .check(importedClasses());
    }

    @Test
    void security_config_no_depende_de_clases_con_nombre_keycloak() {
        noClasses()
                .that().haveFullyQualifiedName(SECURITY_CONFIG_CLASS)
                .should().dependOnClassesThat().haveNameMatching(".*[Kk]eycloak.*")
                .check(importedClasses());
    }

    // 42: Application -> Keycloak = 0 ; Application -> Jwt = 0 ; Application -> Spring Security = 0
    @Test
    void application_no_depende_de_keycloak_ni_jwt_ni_spring_security() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage("..keycloak..", "org.springframework.security..")
                .check(importedClasses());

        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().haveNameMatching(".*[Kk]eycloak.*|org\\.springframework\\.security\\.oauth2\\.jwt\\..*")
                .check(importedClasses());
    }

    // 43: Controllers no dependen de JwtClaimsExtractor/KeycloakJwtClaimsExtractor/IdentityProviderPort.
    // Sí pueden depender de AuthenticatedUserResolver, InputPorts y modelos HTTP.
    @Test
    void controllers_no_dependen_de_jwt_claims_adapter_ni_identity_provider_port() {
        noClasses()
                .that().resideInAPackage(CONTROLLER_PACKAGE)
                .should().dependOnClassesThat().haveNameMatching(
                        ".*JwtClaimsExtractor|.*KeycloakJwtClaimsExtractor|.*IdentityProviderPort"
                )
                .check(importedClasses());
    }

    @Test
    void controllers_no_dependen_de_paquetes_keycloak() {
        noClasses()
                .that().resideInAPackage(CONTROLLER_PACKAGE)
                .should().dependOnClassesThat().resideInAnyPackage("..keycloak..")
                .check(importedClasses());
    }

    // Complemento al 16: KeycloakGrantedAuthoritiesConverter debe haber desaparecido.
    @Test
    void keycloak_granted_authorities_converter_no_existe() {
        noClasses()
                .should().haveSimpleName("KeycloakGrantedAuthoritiesConverter")
                .check(importedClasses());
    }

    private static JavaClasses importedClasses() {
        return new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE);
    }
}
