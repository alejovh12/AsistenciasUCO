package co.edu.uco.asistenciasuco.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * RED causal LB-004B.2 STORAGE-001. Guardrails del desacople de storage respecto a MinIO,
 * descrito en docs/work-items/LB-004-stateless-serverless-readiness/MINIO_STORAGE_CONTRACT.md:
 * el unico componente que conoce el SDK de MinIO es {@code MinioFileStorageAdapter}; el unico
 * que conoce el protocolo clamd/ClamAV es {@code ClamAvMalwareScanAdapter}; ni el Controller ni
 * Application conocen filesystem directo, bucket ni objectKey.
 */
class StorageProviderIsolationRulesTest {

    private static final String BASE_PACKAGE = "co.edu.uco.asistenciasuco";
    private static final String ARCHIVO_CONTROLLER_CLASS =
            "co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.archivo.ArchivoController";
    private static final String MINIO_ADAPTER_CLASS =
            "co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.storage.minio.MinioFileStorageAdapter";
    private static final String CLAMAV_ADAPTER_CLASS =
            "co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.malwarescan.clamav.ClamAvMalwareScanAdapter";

    @Test
    void archivo_controller_no_depende_de_filesystem_directo() {
        noClasses()
                .that().haveFullyQualifiedName(ARCHIVO_CONTROLLER_CLASS)
                .should().dependOnClassesThat().resideInAnyPackage("java.nio.file..")
                .check(importedClasses());
    }

    @Test
    void archivo_controller_no_depende_del_sdk_minio() {
        noClasses()
                .that().haveFullyQualifiedName(ARCHIVO_CONTROLLER_CLASS)
                .should().dependOnClassesThat().resideInAnyPackage("io.minio..")
                .check(importedClasses());
    }

    @Test
    void application_no_depende_del_sdk_minio() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage("io.minio..")
                .check(importedClasses());
    }

    @Test
    void application_no_depende_de_clases_con_nombre_minio_o_clamav() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().haveNameMatching(".*[Mm]inio.*|.*ClamAv.*")
                .check(importedClasses());
    }

    // Dentro de los adapters (primary/secondary), solo el adapter de storage/malware-scan
    // dedicado conoce su SDK/protocolo. El Composition Root (..infrastructure.config..) queda
    // fuera de esta regla a proposito: construir el adapter concreto es exactamente su trabajo.
    @Test
    void solo_minio_file_storage_adapter_conoce_el_sdk_minio_entre_los_adapters() {
        noClasses()
                .that().resideInAPackage("..infrastructure.adapter..")
                .and(DescribedPredicate.not(HasName.Predicates.name(MINIO_ADAPTER_CLASS)))
                .should().dependOnClassesThat().resideInAnyPackage("io.minio..")
                .check(importedClasses());
    }

    @Test
    void solo_clamav_adapter_conoce_el_cliente_socket_clamd_entre_los_adapters() {
        noClasses()
                .that().resideInAPackage("..infrastructure.adapter..")
                .and(DescribedPredicate.not(HasName.Predicates.name(CLAMAV_ADAPTER_CLASS)))
                .should().dependOnClassesThat().haveNameMatching(".*ClamAv.*")
                .check(importedClasses());
    }

    private static JavaClasses importedClasses() {
        return new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE);
    }
}


