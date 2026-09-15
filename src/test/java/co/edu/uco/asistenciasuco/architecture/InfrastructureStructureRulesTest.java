package co.edu.uco.asistenciasuco.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Guardrails específicos del árbol de infraestructura descrito en
 * docs/architecture/infrastructure-structure.md (refactor estructural de infrastructure y
 * crosscutting). Estas reglas existen para que la estructura que este refactor estableció no se
 * erosione silenciosamente con el tiempo.
 */
class InfrastructureStructureRulesTest {

    private static final String BASE_PACKAGE = "co.edu.uco.asistenciasuco";

    @Test
    void ninguna_interfaz_productiva_termina_en_adapter() {
        // "Adapter" identifica una IMPLEMENTACIÓN técnica concreta, nunca un contrato. Si algo
        // que termina en Adapter es una interfaz, el nombre correcto es Port (contrato de
        // Application), o un contrato interno con sufijo Gateway/Resolver/Extractor/Mapper.
        classes()
                .that().haveSimpleNameEndingWith("Adapter")
                .and().areInterfaces()
                .should().notBeInterfaces()
                .allowEmptyShould(true)
                .check(importedClasses());
    }

    @Test
    void ningun_mock_adapter_vive_en_produccion() {
        // Los *MockAdapter son test doubles: deben vivir únicamente en src/test/java, nunca ser
        // seleccionables desde una configuración de producción (ver
        // infrastructure.adapter.secondary.persistence.sqlserver.testdouble en el árbol de test).
        noClasses()
                .that().haveSimpleNameEndingWith("MockAdapter")
                .should().resideOutsideOfPackage("..testdouble..")
                .allowEmptyShould(true)
                .check(importedClasses());
    }

    @Test
    void el_anidamiento_repository_adapter_no_reaparece() {
        // Antes de este refactor, infrastructure.adapter.secondary.repository.adapter mezclaba
        // "repository" (concepto) y "adapter" (rol) de forma redundante. La consolidación bajo
        // persistence.sqlserver.{core,academic,reporting,authorization,support} no debe
        // deshacerse accidentalmente reintroduciendo ese paquete.
        noClasses()
                .that().resideInAPackage(BASE_PACKAGE + "..")
                .should().resideInAPackage("..infrastructure.adapter.secondary.repository.adapter..")
                .check(importedClasses());
    }

    @Test
    void application_no_conoce_vendors_de_observabilidad() {
        // Prometheus/Grafana/Loki/Tempo/Alloy son estándares de plataforma consumidos vía
        // Micrometer/OpenTelemetry desde infrastructure; Application jamás debe importarlos
        // directamente ni tener un Port dedicado a ellos (ver sección "Neutralidad de vendor de
        // observabilidad" en docs/architecture/infrastructure-structure.md).
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..prometheus..",
                        "..grafana..",
                        "..loki..",
                        "..tempo..",
                        "..alloy.."
                )
                .check(importedClasses());
    }

    @Test
    void ningun_provider_especifico_de_observabilidad_vive_dentro_de_application() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().haveSimpleNameContaining("Prometheus")
                .orShould().haveSimpleNameContaining("Grafana")
                .orShould().haveSimpleNameContaining("Loki")
                .orShould().haveSimpleNameContaining("Tempo")
                .check(importedClasses());
    }

    private static JavaClasses importedClasses() {
        return new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE);
    }
}
