package co.edu.uco.asistenciasuco.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Congela la frontera de {@code coordinador.common}: solo contiene modelos con
 * consumidores reales en mas de una vertical de caso de uso.
 */
class CoordinadorCommonStructureTest {

    private static final Set<String> INTENTIONALLY_SHARED = Set.of(
            "co.edu.uco.asistenciasuco.application.features.coordinador.common.domain.DocenteProgramaDomain",
            "co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.AsignaturaDTO",
            "co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.DocenteProgramaDTO",
            "co.edu.uco.asistenciasuco.application.features.coordinador.common.mapper.DocenteProgramaMapper"
    );

    @Test
    void common_only_contains_intentionally_shared_coordinator_models() {
        classes()
                .that().resideInAPackage("..application.features.coordinador.common..")
                .should(beIntentionallyShared())
                .check(importedClasses());
    }

    private static ArchCondition<JavaClass> beIntentionallyShared() {
        return new ArchCondition<>("be an intentionally shared coordinator model") {
            @Override
            public void check(final JavaClass item, final ConditionEvents events) {
                if (!INTENTIONALLY_SHARED.contains(item.getFullName())) {
                    events.add(SimpleConditionEvent.violated(item,
                            item.getFullName() + " no es compartido intencionalmente y debe vivir en su vertical"));
                }
            }
        };
    }

    private static JavaClasses importedClasses() {
        return new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("co.edu.uco.asistenciasuco");
    }
}
