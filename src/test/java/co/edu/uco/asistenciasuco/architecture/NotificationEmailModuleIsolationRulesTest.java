package co.edu.uco.asistenciasuco.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * TEST_PLAN §3 (AR-07..AR-10): guardrails de removibilidad y aislamiento del modulo de plantillas,
 * contacto y canal SMTP (NOTIF-002A).
 */
class NotificationEmailModuleIsolationRulesTest {

    private static final String BASE_PACKAGE = "co.edu.uco.asistenciasuco";
    private static final String TEMPLATE_PKG = "..infrastructure.adapter.secondary.notification.template..";
    private static final String CONTACT_PKG = "..infrastructure.adapter.secondary.notification.contact..";
    private static final String SMTP_PKG = "..infrastructure.adapter.secondary.notification.channel.smtp..";
    private static final String NOTIF_CONFIG_PKG = "..infrastructure.config.adapters.notification..";

    private static JavaClasses importedClasses() {
        return new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE);
    }

    @Test
    @DisplayName("AR-07: solo config.adapters.notification y el propio paquete notification referencian clases de template/contact/channel.smtp")
    void AR07_solo_config_y_notification_referencian_el_modulo() {
        noClasses()
                .that().resideOutsideOfPackages(
                        TEMPLATE_PKG,
                        CONTACT_PKG,
                        SMTP_PKG,
                        NOTIF_CONFIG_PKG
                )
                .should().dependOnClassesThat().resideInAnyPackage(TEMPLATE_PKG, CONTACT_PKG, SMTP_PKG)
                .check(importedClasses());
    }

    @Test
    @DisplayName("AR-08: application.. y controladores no dependen de notification.template, contact ni channel.smtp")
    void AR08_application_y_controladores_no_dependen_del_modulo() {
        noClasses()
                .that().resideInAnyPackage("..application..", "..infrastructure.adapter.primary..")
                .should().dependOnClassesThat().resideInAnyPackage(TEMPLATE_PKG, CONTACT_PKG, SMTP_PKG)
                .check(importedClasses());
    }

    @Test
    @DisplayName("AR-09: notification.template y notification.contact no dependen de channel.smtp (dependencia en un solo sentido)")
    void AR09_template_y_contact_no_dependen_de_channel_smtp() {
        noClasses()
                .that().resideInAnyPackage(TEMPLATE_PKG, CONTACT_PKG)
                .should().dependOnClassesThat().resideInAnyPackage(SMTP_PKG)
                .check(importedClasses());
    }

    @Test
    @DisplayName("AR-10: sin @Component/@Service/@Repository en clases de template/contact/channel.smtp y sin JdbcTemplate")
    void AR10_sin_estereotipos_spring_ni_jdbc_template_en_modulo() {
        classes()
                .that().resideInAnyPackage(TEMPLATE_PKG, CONTACT_PKG, SMTP_PKG)
                .should().notBeAnnotatedWith(Component.class)
                .andShould().notBeAnnotatedWith(Service.class)
                .andShould().notBeAnnotatedWith(Repository.class)
                .check(importedClasses());

        noClasses()
                .that().resideInAnyPackage(TEMPLATE_PKG, CONTACT_PKG, SMTP_PKG)
                .should().dependOnClassesThat().areAssignableTo(JdbcTemplate.class)
                .check(importedClasses());
    }
}
