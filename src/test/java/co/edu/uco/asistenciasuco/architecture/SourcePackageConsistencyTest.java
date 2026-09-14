package co.edu.uco.asistenciasuco.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SourcePackageConsistencyTest {

    private static final Path SOURCE_ROOT = Path.of("src/main/java");
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("(?m)^\\s*package\\s+([a-zA-Z0-9_.]+)\\s*;");

    @Test
    void source_path_refleja_package_java_declarado() throws IOException {
        final List<String> mismatches;
        try (var files = Files.walk(SOURCE_ROOT)) {
            mismatches = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(SourcePackageConsistencyTest::packageMismatch)
                    .flatMap(List::stream)
                    .sorted()
                    .toList();
        }

        assertTrue(mismatches.isEmpty(), "Packages Java desalineados con src/main/java: " + mismatches);
    }

    private static List<String> packageMismatch(final Path sourceFile) {
        try {
            final String source = Files.readString(sourceFile);
            final var matcher = PACKAGE_PATTERN.matcher(source);
            if (!matcher.find()) {
                return List.of();
            }
            final String declaredPackage = matcher.group(1);
            final Path relativeDirectory = SOURCE_ROOT.relativize(sourceFile).getParent();
            final String expectedPackage = relativeDirectory.toString().replace('\\', '.').replace('/', '.');
            if (declaredPackage.equals(expectedPackage)) {
                return List.of();
            }
            return List.of(sourceFile + " declara " + declaredPackage + " pero vive en " + expectedPackage);
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible inspeccionar " + sourceFile, exception);
        }
    }
}
