package co.edu.uco.asistenciasuco.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guardrails del estándar de Composition Root descrito en
 * docs/architecture/adapter-composition-standard.md.
 *
 * <p>Objetivo: Domain y Application no conocen tecnologías concretas; la selección de
 * tecnología ocurre una sola vez, al iniciar Spring, en infrastructure.config.adapters.*;
 * y los Feature Configs solo conectan Application (UseCase/InputPort), nunca instancian
 * adapters tecnológicos directamente.</p>
 */
class AdapterCompositionRootRulesTest {

    private static final String BASE_PACKAGE = "co.edu.uco.asistenciasuco";
    private static final String DOMAIN_PACKAGE = "..application..domain..";
    private static final String FEATURE_CONFIG_PACKAGE = "..infrastructure.config.features..";
    private static final String SECONDARY_ADAPTER_PACKAGE = "..infrastructure.adapter.secondary..";

    // 19.1: Feature Configs solo conectan Application; no instancian adapters tecnológicos.
    @Test
    void feature_configs_no_dependen_de_adapters_secundarios() {
        noClasses()
                .that().resideInAPackage(FEATURE_CONFIG_PACKAGE)
                .should().dependOnClassesThat().resideInAPackage(SECONDARY_ADAPTER_PACKAGE)
                .check(importedClasses());
    }

    // 19.2: Application no debe conocer proveedores tecnológicos concretos.
    @Test
    void application_no_depende_de_paquetes_de_proveedores_tecnologicos() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..keycloak..",
                        "..sqlserver..",
                        "..redis..",
                        "..rabbit..",
                        "..minio..",
                        "..vault.."
                )
                .check(importedClasses());
    }

    // 19.3: Domain (usecase.domain) tampoco debe conocer proveedores tecnológicos concretos.
    @Test
    void domain_no_depende_de_paquetes_de_proveedores_tecnologicos() {
        noClasses()
                .that().resideInAPackage(DOMAIN_PACKAGE)
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..keycloak..",
                        "..sqlserver..",
                        "..redis..",
                        "..rabbit..",
                        "..minio..",
                        "..vault.."
                )
                .check(importedClasses());
    }

    // 19.4: Los adapters SQL Server seleccionables por el Composition Root no deben
    // autoregistrarse mediante component scanning; los registra explícitamente
    // infrastructure.config.adapters.*.
    @Test
    void sqlserver_adapters_no_se_autoregistran_con_stereotypes_de_spring() {
        classes()
                .that().haveSimpleNameEndingWith("SqlServerAdapter")
                .should().notBeAnnotatedWith(org.springframework.stereotype.Repository.class)
                .andShould().notBeAnnotatedWith(org.springframework.stereotype.Component.class)
                .andShould().notBeAnnotatedWith(org.springframework.stereotype.Service.class)
                .check(importedClasses());
    }

    // 19.4 (complemento): colaboradores JDBC seleccionables tampoco deben autoregistrarse.
    @Test
    void audit_event_jdbc_repository_no_se_autoregistra_con_stereotypes_de_spring() {
        classes()
                .that().haveSimpleName("AuditEventJdbcRepository")
                .should().notBeAnnotatedWith(org.springframework.stereotype.Repository.class)
                .andShould().notBeAnnotatedWith(org.springframework.stereotype.Component.class)
                .andShould().notBeAnnotatedWith(org.springframework.stereotype.Service.class)
                .check(importedClasses());
    }

    // 19.4 (complemento): tampoco deben seleccionarse mediante @Profile tecnológico.
    @Test
    void adapters_secundarios_no_usan_profile_para_seleccion_tecnologica() {
        noClasses()
                .that().resideInAPackage(SECONDARY_ADAPTER_PACKAGE)
                .should().beAnnotatedWith(org.springframework.context.annotation.Profile.class)
                .check(importedClasses());

        noClasses()
                .that().resideInAPackage(FEATURE_CONFIG_PACKAGE)
                .should().beAnnotatedWith(org.springframework.context.annotation.Profile.class)
                .check(importedClasses());
    }

    // Las configuraciones del Composition Root deben residir bajo infrastructure.config.adapters
    // y no bajo infrastructure.config.features (separación Feature Config vs Adapter Config).
    @Test
    void adapter_configurations_residen_en_composition_root() {
        classes()
                .that().haveSimpleNameEndingWith("AdapterConfiguration")
                .should().resideInAPackage("..infrastructure.config.adapters..")
                .check(importedClasses());
    }

    @Test
    void configuraciones_jdbc_residen_en_composition_root_de_persistencia() {
        final List<String> offenders = importedClasses().stream()
                .filter(javaClass -> javaClass.getPackageName().contains(".infrastructure.config."))
                .filter(javaClass -> !javaClass.getPackageName().contains(".infrastructure.config.adapters.persistence."))
                .filter(AdapterCompositionRootRulesTest::dependsOnJdbc)
                .map(JavaClass::getName)
                .sorted()
                .toList();

        assertTrue(offenders.isEmpty(), "Configuraciones JDBC fuera del Composition Root de persistencia: " + offenders);
    }

    private static boolean dependsOnJdbc(final JavaClass javaClass) {
        return javaClass.getDirectDependenciesFromSelf().stream()
                .map(dependency -> dependency.getTargetClass().getName())
                .anyMatch(AdapterCompositionRootRulesTest::isJdbcType);
    }

    private static boolean isJdbcType(final String className) {
        return className.startsWith("org.springframework.jdbc.")
                || className.startsWith("javax.sql.")
                || className.startsWith("jakarta.sql.");
    }

    private static JavaClasses importedClasses() {
        return new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE);
    }
}
