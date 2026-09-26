package co.edu.uco.asistenciasuco.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * LB-002.1: JPA/Hibernate/Spring Data solo pueden existir en Infrastructure. Domain y Application
 * (incluidos puertos, DTO y proyecciones) permanecen neutrales. Los modelos JPA son modelos de
 * persistencia de solo lectura (vistas) ubicados bajo persistence.sqlserver.jpa.
 */
class JpaIsolationRulesTest {

    private static final String BASE_PACKAGE = "co.edu.uco.asistenciasuco";
    private static final String[] JPA_PACKAGES = {
            "jakarta.persistence..",
            "org.springframework.data..",
            "org.hibernate.."
    };

    @Test
    void application_no_depende_de_jpa_hibernate_ni_spring_data() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage(JPA_PACKAGES)
                .check(importedClasses());
    }

    @Test
    void domain_no_depende_de_jpa_hibernate_ni_spring_data() {
        noClasses()
                .that().resideInAPackage("..application..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(JPA_PACKAGES)
                .check(importedClasses());
    }

    @Test
    void crosscutting_y_controllers_no_dependen_de_jpa() {
        noClasses()
                .that().resideInAnyPackage("..crosscutting..", "..infrastructure.adapter.primary..")
                .should().dependOnClassesThat().resideInAnyPackage(JPA_PACKAGES)
                .check(importedClasses());
    }

    @Test
    void entidades_jpa_solo_viven_en_infrastructure_persistence_jpa_y_son_inmutables() {
        classes()
                .that().areAnnotatedWith(jakarta.persistence.Entity.class)
                .should().resideInAPackage("..infrastructure.adapter.secondary.persistence.sqlserver.jpa..")
                .andShould().beAnnotatedWith(org.hibernate.annotations.Immutable.class)
                .allowEmptyShould(true)
                .check(importedClasses());
    }

    @Test
    void modelo_jpa_no_expone_relaciones_ni_cascadas() {
        noClasses()
                .that().resideInAPackage("..infrastructure.adapter.secondary.persistence.sqlserver.jpa..")
                .should().dependOnClassesThat().areAssignableTo(jakarta.persistence.CascadeType.class)
                .orShould().dependOnClassesThat().areAssignableTo(jakarta.persistence.OneToMany.class)
                .orShould().dependOnClassesThat().areAssignableTo(jakarta.persistence.ManyToMany.class)
                .orShould().dependOnClassesThat().areAssignableTo(jakarta.persistence.ManyToOne.class)
                .allowEmptyShould(true)
                .check(importedClasses());
    }

    @Test
    void jpa_no_expone_api_de_escritura_via_spring_data_repository() {
        noClasses()
                .that().resideInAPackage(BASE_PACKAGE + "..")
                .should().dependOnClassesThat().areAssignableTo(org.springframework.data.repository.Repository.class)
                .check(importedClasses());
    }

    private static JavaClasses importedClasses() {
        return new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE);
    }
}
