package co.edu.uco.asistenciasuco.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class MessagingIsolationArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("co.edu.uco.asistenciasuco");
    }

    @Test
    @DisplayName("AR-01: application no depende de infrastructure..notification..")
    void AR01_application_no_depende_de_infrastructure_notification() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..notification..")
                .check(importedClasses);
    }

    @Test
    @DisplayName("AR-02: controllers no dependen de notification outbox ni composite")
    void AR02_controllers_no_dependen_de_outbox_o_composite() {
        noClasses()
                .that().areAnnotatedWith(RestController.class)
                .should().dependOnClassesThat().resideInAnyPackage("..notification.outbox..", "..notification.composite..")
                .check(importedClasses);
    }

    @Test
    @DisplayName("AR-03: adaptadores seleccionables no llevan @Component, @Service ni @Repository")
    void AR03_adaptadores_seleccionables_sin_estereotipos_spring() {
        classes()
                .that().haveSimpleNameEndingWith("NotificacionAdapter")
                .and().haveSimpleNameNotEndingWith("Configuration")
                .should().notBeAnnotatedWith(Component.class)
                .andShould().notBeAnnotatedWith(Service.class)
                .andShould().notBeAnnotatedWith(Repository.class)
                .check(importedClasses);
    }

    @Test
    @DisplayName("AR-05: no existe Map<String, NotificacionPort> en notification")
    void AR05_sin_service_locator() {
        noClasses()
                .that().resideInAPackage("..notification..")
                .should().dependOnClassesThat().haveFullyQualifiedName("org.springframework.context.ApplicationContext")
                .check(importedClasses);
    }
}
