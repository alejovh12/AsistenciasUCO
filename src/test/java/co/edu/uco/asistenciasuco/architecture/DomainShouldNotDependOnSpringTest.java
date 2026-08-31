package co.edu.uco.asistenciasuco.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class DomainShouldNotDependOnSpringTest {

    private static final String BASE_PACKAGE = "co.edu.uco.asistenciasuco";
    private static final String DOMAIN_PACKAGE = "..application..domain..";

    @Test
    void usecase_domain_should_not_depend_on_spring() {
        var classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE);

        noClasses()
                .that().resideInAPackage(DOMAIN_PACKAGE)
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
                .check(classes);
    }

    @Test
    void usecase_domain_should_not_depend_on_jakarta_or_javax() {
        var classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE);

        noClasses()
                .that().resideInAPackage(DOMAIN_PACKAGE)
                .should().dependOnClassesThat().resideInAnyPackage("jakarta..", "javax..")
                .check(classes);
    }

    @Test
    void ports_and_dto_should_be_in_expected_packages() {
        var classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE);

        classes()
                .that().haveSimpleNameEndingWith("Interactor")
                .should().resideInAPackage("..primaryports.interactor..")
                .check(classes);

        classes()
                .that().haveSimpleNameEndingWith("DTO")
                .and().resideInAPackage("..application.features..")
                .should().resideInAPackage("..primaryports.dto..")
                .check(classes);

        classes()
                .that().haveSimpleNameEndingWith("InputPort")
                .should().resideInAPackage("..primaryports..")
                .check(classes);

        classes()
                .that().haveSimpleNameEndingWith("RepositoryPort")
                .should().resideInAPackage("..application.secondaryports..")
                .check(classes);
    }

    @Test
    void util_should_not_depend_on_infrastructure() {
        var classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE);

        noClasses()
                .that().resideInAPackage("..crosscutting.helpers..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .check(classes);
    }
}
