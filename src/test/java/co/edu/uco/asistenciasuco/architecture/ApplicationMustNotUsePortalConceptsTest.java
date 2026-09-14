package co.edu.uco.asistenciasuco.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationMustNotUsePortalConceptsTest {

    @Test
    void application_no_contiene_secondaryports_portal_ni_modelos_portal() throws IOException {
        try (var files = Files.walk(Path.of("src/main/java/co/edu/uco/asistenciasuco/application"))) {
            final boolean hasPortalConcept = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .anyMatch(ApplicationMustNotUsePortalConceptsTest::containsPortalConcept);
            assertTrue(!hasPortalConcept);
        }
    }

    @Test
    void application_no_contiene_packages_ni_paths_portal() throws IOException {
        try (var files = Files.walk(Path.of("src/main/java/co/edu/uco/asistenciasuco/application"))) {
            final boolean hasPortalPathOrPackage = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .anyMatch(ApplicationMustNotUsePortalConceptsTest::containsPortalPathOrPackage);
            assertTrue(!hasPortalPathOrPackage);
        }
    }

    private static boolean containsPortalConcept(final Path path) {
        try {
            final String source = Files.readString(path);
            return source.contains("secondaryports.portal")
                    || source.contains("PortalPort")
                    || source.contains("PortalRecord")
                    || source.contains("PortalDTO");
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible inspeccionar " + path, exception);
        }
    }

    private static boolean containsPortalPathOrPackage(final Path path) {
        try {
            final String normalizedPath = path.toString().replace('\\', '/');
            return normalizedPath.contains("/portal/")
                    || Files.readString(path).contains(".portal.");
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible inspeccionar " + path, exception);
        }
    }
}
