package co.edu.uco.asistenciasuco.architecture;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * INT-003 (Trivy del PR #150): {@code pulsar-client} 4.1.x trae {@code org.apache.pulsar:bouncy-castle-bc} (clasificador
 * {@code pkg}), que empaqueta un {@code bcprov-jdk18on-1.78.1.jar} anidado con CVE criticas (CVE-2025-14813,
 * CVE-2026-8763...). El Backend solo debe llevar el Bouncy Castle alineado por {@code bc-jdk18on-bom} (SEC-002).
 */
class BouncyCastleClasspathTest {

    private static List<String> classpath() {
        return Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .map(ruta -> ruta.replace('\\', '/'))
                .map(ruta -> ruta.substring(ruta.lastIndexOf('/') + 1))
                .toList();
    }

    @Test
    void no_entra_el_paquete_de_bouncy_castle_de_pulsar() {
        final List<String> paquetes = classpath().stream().filter(jar -> jar.startsWith("bouncy-castle-bc")).toList();
        assertTrue(paquetes.isEmpty(), () -> "bouncy-castle-bc embebe un bcprov viejo: " + paquetes);
    }

    @Test
    void solo_hay_un_bcprov_y_es_el_de_la_bom() {
        final List<String> bcprov = classpath().stream().filter(jar -> jar.startsWith("bcprov-")).toList();
        assertEquals(1, bcprov.size(), () -> "bcprov en el classpath: " + bcprov);
        final String jar = BouncyCastleProvider.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        assertTrue(jar.endsWith("bcprov-jdk18on-1.85.2.jar"), () -> "BouncyCastleProvider viene de " + jar);
    }
}
