package co.edu.uco.asistenciasuco.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
    void application_no_depende_de_reactor() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "reactor..",
                        "org.reactivestreams.."
                )
                .check(importedClasses());
    }

    @Test
    void application_no_depende_de_webflux_reactivo() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.web.reactive..",
                        "org.springframework.http.codec.."
                )
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
    void controllers_no_dependen_de_jdbc_ni_java_sql() {
        noClasses()
                .that().resideInAPackage("..infrastructure.adapter.primary.controller..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.jdbc.core..",
                        "org.springframework.jdbc.core.namedparam..",
                        "org.springframework.jdbc.core.simple..",
                        "java.sql.."
                )
                .check(importedClasses());
    }

    @Test
    void codigo_productivo_no_referencia_procedimientos_internos() throws IOException {
        try (var files = Files.walk(Path.of("src/main/java"))) {
            final boolean hasInternalProcedure = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .anyMatch(path -> containsInternalProcedureReference(path));
            assertTrue(!hasInternalProcedure);
        }
    }

    private static boolean containsInternalProcedureReference(final Path path) {
        try {
            return Files.readString(path).matches("(?s).*usp_.*_interno.*");
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible inspeccionar " + path, exception);
        }
    }

    @Test
    void adapters_secondary_en_infrastructure_adapter_secondary() {
        classes()
                .that().haveSimpleNameEndingWith("Adapter")
                .and().resideInAPackage("..infrastructure..")
                .and().resideOutsideOfPackage("..infrastructure.adapter.primary.security..")
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
                .should().resideInAnyPackage("..primaryports.dto..", "..application.features..common.dto..")
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

    @Test
    void usecase_no_depende_de_primaryports_dto() {
        noClasses()
                .that().resideInAPackage("..application.features..usecase..")
                .should().dependOnClassesThat().resideInAPackage("..primaryports.dto..")
                .check(importedClasses());
    }

    @Test
    void usecaseimpl_implementa_interfaz_usecase() {
        classes()
                .that().resideInAPackage("..application.features..usecase.impl..")
                .and().haveSimpleNameEndingWith("UseCaseImpl")
                .should(implementarUnaInterfazUseCase())
                .check(importedClasses());
    }

    @Test
    void controller_no_depende_de_usecaseimpl() {
        noClasses()
                .that().resideInAPackage("..infrastructure.adapter.primary.controller..")
                .should().dependOnClassesThat().haveSimpleNameEndingWith("UseCaseImpl")
                .check(importedClasses());
    }

    @Test
    void domain_no_tiene_anotaciones_de_framework() {
        noClasses()
                .that().resideInAPackage(DOMAIN_PACKAGE)
                .should().beAnnotatedWith(Component.class)
                .orShould().beAnnotatedWith(Service.class)
                .orShould().beAnnotatedWith(Repository.class)
                .orShould().beAnnotatedWith(Configuration.class)
                .orShould().beAnnotatedWith(Autowired.class)
                .check(importedClasses());
    }

    private static ArchCondition<JavaClass> implementarUnaInterfazUseCase() {
        return new ArchCondition<JavaClass>("implement an interface ending with UseCase") {
            @Override
            public void check(final JavaClass item, final ConditionEvents events) {
                final boolean cumple = item.getInterfaces().stream()
                        .anyMatch(interfaz -> interfaz.toErasure().getSimpleName().endsWith("UseCase"));
                events.add(new SimpleConditionEvent(
                        item,
                        cumple,
                        item.getFullName() + (cumple
                                ? " implementa una interfaz UseCase"
                                : " no implementa ninguna interfaz UseCase")
                ));
            }
        };
    }

    private static JavaClasses importedClasses() {
        return new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE);
    }

}
