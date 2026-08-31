package co.edu.uco.asistenciasuco.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CleanArchitectureRulesTest {

    private static final String BASE_PACKAGE = "co.edu.uco.asistenciasuco";
    private static final String DOMAIN_PACKAGE = "..application..domain..";

    @Test
    void application_no_depende_de_infrastructure() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .check(importedClasses());
    }

    @Test
    void domain_no_depende_de_spring() {
        noClasses()
                .that().resideInAPackage(DOMAIN_PACKAGE)
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta..", "javax..")
                .check(importedClasses());
    }

    @Test
    void domain_no_depende_de_output_ports() {
        noClasses()
                .that().resideInAPackage(DOMAIN_PACKAGE)
                .should().dependOnClassesThat().resideInAPackage("..application.secondaryports..")
                .check(importedClasses());
    }

    @Test
    void usecase_impl_no_depende_de_infrastructure() {
        noClasses()
                .that().resideInAPackage("..usecase.impl..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .check(importedClasses());
    }

    @Test
    void infrastructure_puede_depender_de_application() {
        boolean hasDependency = importedClasses().stream()
                .filter(javaClass -> javaClass.getPackageName().contains(".infrastructure."))
                .flatMap(javaClass -> javaClass.getDirectDependenciesFromSelf().stream())
                .anyMatch(dependency -> dependency.getTargetClass().getPackageName().contains(".application."));
        assertTrue(hasDependency);
    }

    @Test
    void adapters_primary_en_infrastructure_adapter_primary() {
        classes()
                .that().areAnnotatedWith(RestController.class)
                .should().resideInAPackage("..infrastructure.adapter.primary.controller..")
                .check(importedClasses());
    }

    @Test
    void adapters_secondary_en_infrastructure_adapter_secondary() {
        classes()
                .that().haveSimpleNameEndingWith("Adapter")
                .and().resideInAPackage("..infrastructure..")
                .should().resideInAPackage("..infrastructure.adapter.secondary..")
                .check(importedClasses());
    }

    @Test
    void tracing_web_en_infrastructure_observability_tracing() {
        boolean hasTracingOutsideInfrastructure = importedClasses().stream()
                .filter(javaClass -> javaClass.getSimpleName().startsWith("Correlation"))
                .anyMatch(javaClass -> !javaClass.getPackageName().contains(".infrastructure."));
        assertTrue(!hasTracingOutsideInfrastructure);
    }

    @Test
    void util_no_depende_de_infrastructure() {
        noClasses()
                .that().resideInAPackage("..crosscutting.helpers..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .check(importedClasses());
    }

    @Test
    void dtos_en_application_ports_input_dto() {
        classes()
                .that().haveSimpleNameEndingWith("DTO")
                .and().resideInAPackage("..application.features..")
                .should().resideInAPackage("..primaryports.dto..")
                .check(importedClasses());
    }

    @Test
    void interactors_en_application_ports_input_interactor() {
        classes()
                .that().haveSimpleNameEndingWith("Interactor")
                .should().resideInAPackage("..primaryports.interactor..")
                .check(importedClasses());
    }

    @Test
    void output_ports_en_application_ports_output() {
        classes()
                .that().haveSimpleNameEndingWith("InputPort")
                .should().resideInAPackage("..primaryports..")
                .check(importedClasses());

        classes()
                .that().haveSimpleNameEndingWith("RepositoryPort")
                .should().resideInAPackage("..application.secondaryports..")
                .check(importedClasses());
    }

    private static JavaClasses importedClasses() {
        return new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE);
    }

}
