package co.edu.uco.asistenciasuco.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class AdaptersShouldNotBeUsedByDomainTest {

    private static final String BASE_PACKAGE = "co.edu.uco.asistenciasuco";

    @Test
    void usecase_domain_should_not_depend_on_infrastructure() {
        var classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE);

        noClasses()
                .that().resideInAPackage("..application.usecase.domain..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .check(classes);
    }

    @Test
    void rest_controllers_should_only_be_in_primary_rest_package() {
        var classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE);

        classes()
                .that().areAnnotatedWith(RestController.class)
                .should().resideInAPackage("..infrastructure.adapter.primary.controller..")
                .check(classes);
    }

    @Test
    void websocket_controllers_should_only_be_in_primary_websocket_package() {
        var classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE);

        classes()
                .that().areAnnotatedWith(Controller.class)
                .and().areNotAnnotatedWith(RestController.class)
                .and().haveSimpleNameEndingWith("Controller")
                .should().resideInAPackage("..infrastructure.adapter.primary.websocket..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void secondary_adapters_should_only_be_in_secondary_package() {
        var classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE);

        classes()
                .that().haveSimpleNameEndingWith("Adapter")
                .and().resideInAPackage("..infrastructure..")
                .should().resideInAPackage("..infrastructure.adapter.secondary..")
                .check(classes);
    }
}
