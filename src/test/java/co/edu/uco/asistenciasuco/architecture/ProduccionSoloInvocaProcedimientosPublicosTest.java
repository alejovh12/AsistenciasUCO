package co.edu.uco.asistenciasuco.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HEX-09 (VAL-oleada8): regla contractual de {@code docs/architecture/adapter-composition-standard.md} ("Java productivo
 * invoca procedimientos publicos {@code usp_*}; no invoca {@code usp_*_interno}"). Los procedimientos internos
 * ({@code usp_*_interno} o {@code usp_internal_*}) son detalles de la BD que solo orquesta el procedimiento publico; si el
 * backend los llamara, se saltaria la transaccion, el ambito y la tupla de salida de la fachada.
 */
class ProduccionSoloInvocaProcedimientosPublicosTest {

    private static final Pattern PROCEDIMIENTO = Pattern.compile("\\busp_[A-Za-z0-9_]+");
    private static final Pattern INTERNO = Pattern.compile("(?i)_interno$|^usp_internal_");
    private static final List<Path> RAICES = List.of(Path.of("src", "main", "java"), Path.of("src", "main", "resources"));

    @Test
    void ningun_archivo_de_produccion_referencia_un_procedimiento_interno() {
        final List<String> infracciones = new ArrayList<>();
        final int[] publicos = {0};
        RAICES.forEach(raiz -> archivosDeTexto(raiz).forEach(archivo -> {
            final List<String> lineas = leer(archivo);
            for (int i = 0; i < lineas.size(); i++) {
                final Matcher matcher = PROCEDIMIENTO.matcher(lineas.get(i));
                while (matcher.find()) {
                    if (esInterno(matcher.group())) {
                        infracciones.add(archivo + ":" + (i + 1) + " -> " + matcher.group());
                    } else {
                        publicos[0]++;
                    }
                }
            }
        }));

        // Asercion positiva: evita un falso verde si el recorrido no encuentra los adaptadores.
        assertTrue(publicos[0] > 100, () -> "se esperaban mas de 100 referencias a procedimientos publicos; hubo " + publicos[0]);
        assertTrue(infracciones.isEmpty(), () -> "Produccion invoca procedimientos internos: " + infracciones);
    }

    @Test
    void el_detector_distingue_procedimientos_internos_de_publicos() {
        assertTrue(esInterno("usp_resolver_institucion_gestor_periodo_interno"));
        assertTrue(esInterno("usp_internal_calcular_cupos"));
        assertTrue(esInterno("usp_validar_sesion_ventana_registro_INTERNO"));
        assertFalse(esInterno("usp_abrir_toma_asistencia"));
        assertFalse(esInterno("usp_consultar_internos_paginado"));
        assertEquals(1, PROCEDIMIENTO.matcher("EXEC dbo.usp_abrir_toma_asistencia @idSesion = ?").results().count());
    }

    private static boolean esInterno(final String procedimiento) {
        return INTERNO.matcher(procedimiento).find();
    }

    private static List<Path> archivosDeTexto(final Path raiz) {
        try (Stream<Path> archivos = Files.walk(raiz)) {
            return archivos.filter(Files::isRegularFile)
                    .filter(archivo -> {
                        final String nombre = archivo.getFileName().toString();
                        return nombre.endsWith(".java") || nombre.endsWith(".sql") || nombre.endsWith(".yml")
                                || nombre.endsWith(".yaml") || nombre.endsWith(".properties") || nombre.endsWith(".xml");
                    })
                    .toList();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> leer(final Path archivo) {
        try {
            return Files.readAllLines(archivo, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
