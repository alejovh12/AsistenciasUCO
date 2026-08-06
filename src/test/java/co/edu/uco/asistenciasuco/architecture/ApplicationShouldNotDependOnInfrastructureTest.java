package co.edu.uco.asistenciasuco.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ApplicationShouldNotDependOnInfrastructureTest {

    private static final String BASE_PACKAGE = "co.edu.uco.asistenciasuco";

    @Test
    void application_should_not_depend_on_infrastructure() {
        var classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE);

        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .check(classes);
    }

    @Test
    void application_should_not_depend_on_spring() {
        var classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE);

        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
                .check(classes);
    }

    @Test
    void usecase_implementations_should_reside_in_usecase_impl_package() {
        var classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE);

        classes()
                .that().haveSimpleNameEndingWith("UseCaseImpl")
                .and().resideInAPackage("..application..")
                .should().resideInAPackage("..usecase.impl..")
                .check(classes);
    }
}
