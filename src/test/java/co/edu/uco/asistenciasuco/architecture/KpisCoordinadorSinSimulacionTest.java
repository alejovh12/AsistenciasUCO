package co.edu.uco.asistenciasuco.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-BE-10 (anti-simulacion): el codigo de KPIs del coordinador no puede fabricar cifras. Los calculos viven en DB; el
 * backend solo mapea. Falla si aparecen porcentajes por constante, redondeos de estimaciones o excepciones tragadas.
 */
class KpisCoordinadorSinSimulacionTest {

    private static final Path BASE = Path.of("src/main/java/co/edu/uco/asistenciasuco");

    private static final List<Path> RAIZ_FEATURES = List.of(
            BASE.resolve("application/features/coordinador/consultarkpis"),
            BASE.resolve("application/features/coordinador/consultaralertatemprana"),
            BASE.resolve("application/features/coordinador/consultarbitacoraestudiante"),
            BASE.resolve("application/features/coordinador/consultarfichaestudiante"));

    private static final List<Path> ARCHIVOS_ADAPTER = List.of(
            BASE.resolve("infrastructure/adapter/secondary/persistence/sqlserver/jpa/repository/CoordinadorIndicadoresJpaRepository.java"),
            BASE.resolve("infrastructure/adapter/primary/controller/coordinador/CoordinadorIndicadoresController.java"),
            BASE.resolve("infrastructure/adapter/primary/controller/coordinador/CoordinadorFichaEstudianteController.java"));

    private static final List<Pattern> PROHIBIDOS = List.of(
            Pattern.compile("Math\\.round\\s*\\("),
            Pattern.compile("\\*\\s*0\\.\\d+"),
            Pattern.compile("catch\\s*\\(\\s*(final\\s+)?Exception\\s+\\w+\\s*\\)"),
            Pattern.compile("\\bnew\\s+Random\\b"),
            Pattern.compile("(?i)\\b(mock|simulad[oa]|fallback|hardcode)\\b"),
            Pattern.compile("=\\s*\\d+\\.\\d+\\s*;"));

    private static List<Path> archivosAnalizados() throws IOException {
        final var lista = new java.util.ArrayList<>(ARCHIVOS_ADAPTER);
        for (final Path raiz : RAIZ_FEATURES) {
            try (Stream<Path> stream = Files.walk(raiz)) {
                stream.filter(p -> p.toString().endsWith(".java")).forEach(lista::add);
            }
        }
        return lista;
    }

    @Test
    void los_archivos_a_analizar_existen() throws IOException {
        final List<Path> archivos = archivosAnalizados();
        assertTrue(archivos.size() >= 30, "Se esperaban al menos 30 archivos de KPIs, hay " + archivos.size());
        archivos.forEach(p -> assertTrue(Files.exists(p), "No existe " + p));
    }

    @Test
    void no_hay_cifras_simuladas_ni_excepciones_tragadas() throws IOException {
        for (final Path archivo : archivosAnalizados()) {
            final String contenido = Files.readString(archivo);
            for (final Pattern patron : PROHIBIDOS) {
                assertFalse(patron.matcher(contenido).find(), archivo + " contiene el patron prohibido " + patron.pattern());
            }
        }
    }
}
