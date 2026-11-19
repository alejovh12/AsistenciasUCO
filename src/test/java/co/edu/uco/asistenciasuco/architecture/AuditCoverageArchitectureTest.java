package co.edu.uco.asistenciasuco.architecture;

import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/**
 * Cobertura de auditoria de los controladores del panel admin: {@link AuditInterceptor} solo audita un metodo si lleva
 * {@link AuditableOperation}, asi que un endpoint mutante nuevo que olvide la anotacion no deja rastro (ver
 * docs/observability/README.md: "Auditoria de negocio conserva su contrato aparte"). Los controladores de solo lectura
 * (AdminConsultasTransversalesController, AdminIndicadoresController, AdminTipoProgramaController) no tienen ningun
 * endpoint POST/PUT/PATCH/DELETE y por eso no aparecen en la regla.
 * <p>
 * Excepcion verificada (2026-11-18): {@code AdminNotificacionController#generarVistaPrevia} es POST solo porque el
 * borrador de asunto/cuerpo no cabe en query string; renderiza y devuelve sin persistir nada (no recibe ni usa un
 * {@code adminId}, a diferencia de cada metodo vecino en la misma clase), asi que no hay accion de negocio que auditar.
 */
class AuditCoverageArchitectureTest {

    private static final String EXCEPCION_VISTA_PREVIA_SIN_PERSISTENCIA =
            "co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.AdminNotificacionController"
                    + ".generarVistaPrevia";

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin");
    }

    private static final DescribedPredicate<JavaMethod> ES_ENDPOINT_MUTANTE = new DescribedPredicate<>(
            "esta anotado con @PostMapping, @PutMapping, @PatchMapping o @DeleteMapping") {
        @Override
        public boolean test(final JavaMethod method) {
            final boolean esMutante = method.isAnnotatedWith(PostMapping.class)
                    || method.isAnnotatedWith(PutMapping.class)
                    || method.isAnnotatedWith(PatchMapping.class)
                    || method.isAnnotatedWith(DeleteMapping.class);
            final String nombreCalificado = method.getOwner().getFullName() + "." + method.getName();
            return esMutante && !nombreCalificado.equals(EXCEPCION_VISTA_PREVIA_SIN_PERSISTENCIA);
        }
    };

    @Test
    @DisplayName("AUD-01: todo endpoint POST/PUT/PATCH/DELETE del panel admin lleva @AuditableOperation")
    void AUD01_endpoints_mutantes_del_admin_llevan_auditable_operation() {
        methods()
                .that(ES_ENDPOINT_MUTANTE)
                .should().beAnnotatedWith(AuditableOperation.class)
                .because("sin @AuditableOperation, AuditInterceptor no publica el evento: el endpoint queda sin "
                        + "rastro en /app/admin/sistema/auditoria y en dbo.AuditoriaEvento")
                .check(importedClasses);
    }
}
