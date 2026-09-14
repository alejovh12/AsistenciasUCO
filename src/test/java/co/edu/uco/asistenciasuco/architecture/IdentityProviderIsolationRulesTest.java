package co.edu.uco.asistenciasuco.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Guardrails de Identity Provisioning descritos en
 * docs/security/keycloak-identity-provider.md.
 *
 * <p>Objetivo: Application → Keycloak = 0; Application no conoce el mecanismo HTTP con el
 * que el adapter habla con el Admin API (token admin, RestClient/WebClient/HttpClient);
 * Controllers no dependen de {@code IdentityProviderPort} directamente (solo a través de un
 * UseCase/InputPort); {@code KeycloakIdentityProviderAdapter} no se autoregistra — lo conecta
 * el Composition Root ({@code KeycloakIdentityAdapterConfiguration}).</p>
 */
class IdentityProviderIsolationRulesTest {

    private static final String BASE_PACKAGE = "co.edu.uco.asistenciasuco";
    private static final String CONTROLLER_PACKAGE = "..infrastructure.adapter.primary.controller..";
    private static final String IDENTITY_PROVIDER_PORT_CLASS =
            "co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort";

    @Test
    void application_no_depende_de_keycloak() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage("..keycloak..")
                .check(importedClasses());

        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().haveNameMatching(".*[Kk]eycloak.*")
                .check(importedClasses());
    }

    @Test
    void application_no_depende_del_mecanismo_http_del_admin_token() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "java.net.http..",
                        "org.springframework.web.client..",
                        "org.springframework.web.reactive.function.client.."
                )
                .check(importedClasses());
    }

    @Test
    void controllers_no_dependen_de_identity_provider_port_directamente() {
        noClasses()
                .that().resideInAPackage(CONTROLLER_PACKAGE)
                .should().dependOnClassesThat().haveFullyQualifiedName(IDENTITY_PROVIDER_PORT_CLASS)
                .check(importedClasses());
    }

    @Test
    void controllers_no_dependen_de_clases_keycloak() {
        noClasses()
                .that().resideInAPackage(CONTROLLER_PACKAGE)
                .should().dependOnClassesThat().haveNameMatching(".*[Kk]eycloak.*")
                .check(importedClasses());
    }

    @Test
    void keycloak_identity_provider_adapter_no_se_autoregistra() {
        classes()
                .that().haveSimpleName("KeycloakIdentityProviderAdapter")
                .should().notBeAnnotatedWith(org.springframework.stereotype.Component.class)
                .andShould().notBeAnnotatedWith(org.springframework.stereotype.Service.class)
                .andShould().notBeAnnotatedWith(org.springframework.stereotype.Repository.class)
                .check(importedClasses());
    }

    @Test
    void identity_provider_port_implementations_residen_en_secondary_identity() {
        classes()
                .that().implement(IDENTITY_PROVIDER_PORT_CLASS)
                .should().resideInAPackage("..infrastructure.adapter.secondary.identity..")
                .check(importedClasses());
    }

    private static JavaClasses importedClasses() {
        return new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE);
    }
}
