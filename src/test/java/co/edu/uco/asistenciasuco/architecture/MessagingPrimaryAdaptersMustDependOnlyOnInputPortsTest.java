package co.edu.uco.asistenciasuco.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * SEC-BASELINE-020: un adaptador primario de mensajeria (p. ej. {@code AssignmentEventsRabbitListener},
 * {@code MessageUcoLabEventListener}) es un webhook: debe llamar a un puerto de entrada de Application, nunca a un
 * puerto secundario ni a un adaptador secundario directamente (docs/architecture/adapter-composition-standard.md).
 */
class MessagingPrimaryAdaptersMustDependOnlyOnInputPortsTest {

    @Test
    void listeners_de_mensajeria_no_dependen_de_secondary_ports_ni_de_adapters_secondary() {
        noClasses()
                .that().resideInAPackage("..infrastructure.adapter.primary.messaging..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..application.secondaryports..",
                        "..infrastructure.adapter.secondary..",
                        "java.sql.."
                )
                .check(new ClassFileImporter()
                        .withImportOption(new ImportOption.DoNotIncludeTests())
                        .importPackages("co.edu.uco.asistenciasuco"));
    }
}
