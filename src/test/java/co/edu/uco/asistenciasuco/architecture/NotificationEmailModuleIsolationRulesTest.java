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
 * TEST_PLAN AR-07..AR-10 (F4): guardrails de removibilidad y aislamiento del modulo de notificaciones. El renderizado y el
 * SMTP viven en el Servicio de Notificaciones; el backend conserva el cliente HTTP ({@code service}), la resolucion de
 * contactos ({@code contact}) y el token de verificacion ({@code verificacion}).
 */
class NotificationEmailModuleIsolationRulesTest {

    private static final String BASE_PACKAGE = "co.edu.uco.asistenciasuco";
    private static final String SERVICE_PKG = "..infrastructure.adapter.secondary.notification.service..";
    private static final String CONTACT_PKG = "..infrastructure.adapter.secondary.notification.contact..";
    private static final String VERIFICACION_PKG = "..infrastructure.adapter.secondary.notification.verificacion..";
    private static final String NOTIF_ADAPTER_PKG = "..infrastructure.adapter.secondary.notification..";
    private static final String NOTIF_CONFIG_PKG = "..infrastructure.config.adapters.notification..";

    private static JavaClasses importedClasses() {
        return new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE);
    }

    @Test
    @DisplayName("AR-07: solo el paquete notification.service y la configuracion de notificaciones referencian el cliente del servicio")
    void AR07_solo_el_adaptador_del_servicio_referencia_el_cliente() {
        noClasses()
                .that().resideOutsideOfPackages(SERVICE_PKG, NOTIF_CONFIG_PKG)
                .should().dependOnClassesThat().resideInAnyPackage(SERVICE_PKG)
                .check(importedClasses());
    }

    @Test
    @DisplayName("AR-07b: contact y verificacion solo los usan el paquete notification y la configuracion de notificaciones")
    void AR07b_contact_y_verificacion_son_internos_del_modulo() {
        noClasses()
                .that().resideOutsideOfPackages(NOTIF_ADAPTER_PKG, NOTIF_CONFIG_PKG)
                .should().dependOnClassesThat().resideInAnyPackage(CONTACT_PKG, VERIFICACION_PKG)
                .check(importedClasses());
    }

    @Test
    @DisplayName("AR-08: application.. y controladores no dependen de notification.service, contact ni verificacion")
    void AR08_application_y_controladores_no_dependen_del_modulo() {
        noClasses()
                .that().resideInAnyPackage("..application..", "..infrastructure.adapter.primary..")
                .should().dependOnClassesThat().resideInAnyPackage(SERVICE_PKG, CONTACT_PKG, VERIFICACION_PKG)
                .check(importedClasses());
    }

    @Test
    @DisplayName("AR-09: contact y verificacion no dependen del cliente del servicio (dependencia en un solo sentido)")
    void AR09_contact_y_verificacion_no_dependen_del_servicio() {
        noClasses()
                .that().resideInAnyPackage(CONTACT_PKG, VERIFICACION_PKG)
                .should().dependOnClassesThat().resideInAnyPackage(SERVICE_PKG)
                .check(importedClasses());
    }

    @Test
    @DisplayName("AR-10: sin @Component/@Service/@Repository en service/contact/verificacion y sin JdbcTemplate")
    void AR10_sin_estereotipos_spring_ni_jdbc_template_en_modulo() {
        classes()
                .that().resideInAnyPackage(SERVICE_PKG, CONTACT_PKG, VERIFICACION_PKG)
                .should().notBeAnnotatedWith(Component.class)
                .andShould().notBeAnnotatedWith(Service.class)
                .andShould().notBeAnnotatedWith(Repository.class)
                .check(importedClasses());

        noClasses()
                .that().resideInAnyPackage(SERVICE_PKG, CONTACT_PKG, VERIFICACION_PKG)
                .should().dependOnClassesThat().areAssignableTo(JdbcTemplate.class)
                .check(importedClasses());
    }
}
