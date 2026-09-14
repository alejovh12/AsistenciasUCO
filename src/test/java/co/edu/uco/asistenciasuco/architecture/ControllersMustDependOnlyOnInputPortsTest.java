package co.edu.uco.asistenciasuco.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ControllersMustDependOnlyOnInputPortsTest {

    @Test
    void controllers_no_dependen_de_secondary_ports_adapters_jdbc_ni_java_sql() {
        noClasses()
                .that().resideInAPackage("..infrastructure.adapter.primary.controller..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..application.secondaryports..",
                        "..infrastructure.adapter.secondary..",
                        "org.springframework.jdbc.core..",
                        "org.springframework.jdbc.core.namedparam..",
                        "org.springframework.jdbc.core.simple..",
                        "java.sql.."
                )
                .check(new ClassFileImporter()
                        .withImportOption(new ImportOption.DoNotIncludeTests())
                        .importPackages("co.edu.uco.asistenciasuco"));
    }
}
