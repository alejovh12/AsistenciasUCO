package co.edu.uco.asistenciasuco.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * LB-008 JPA-02A: existe UN solo bootstrap JPA, el de Spring Boot. Ninguna clase de produccion crea el
 * {@code EntityManagerFactory} a mano, envuelve un {@code EntityManager} compartido manualmente ni abre
 * {@code EntityManager} desde la factory. Los adapters JPA reciben el {@code EntityManager} administrado
 * por Spring. Estas reglas no relajan {@link JpaIsolationRulesTest} ni {@link JpaCommandIsolationRulesTest}.
 */
class JpaBootstrapStandardRulesTest {

    private static final String BASE_PACKAGE = "co.edu.uco.asistenciasuco";
    private static final String JPA_ADAPTER_PACKAGE = "..infrastructure.adapter.secondary.persistence.sqlserver.jpa..";

    @Test
    void produccion_no_define_emf_manual_con_local_container_entity_manager_factory_bean() {
        noClasses()
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean")
                .check(importedClasses());
    }

    @Test
    void produccion_no_crea_shared_entity_manager_manualmente() {
        noClasses()
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.orm.jpa.SharedEntityManagerCreator")
                .check(importedClasses());
    }

    @Test
    void adapters_jpa_no_reciben_ni_usan_el_entity_manager_factory() {
        noClasses()
                .that().resideInAPackage(JPA_ADAPTER_PACKAGE)
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("jakarta.persistence.EntityManagerFactory")
                .check(importedClasses());
    }

    private static JavaClasses importedClasses() {
        return new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }
}


