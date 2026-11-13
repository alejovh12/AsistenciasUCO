package co.edu.uco.asistenciasuco.application.exception.catalog;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * VAL-oleada3 MSG-03 y MSG-05: los codigos de mensaje que usa el backend existen en el catalogo de usuario.
 *
 * <p>El catalogo es el fixture {@code messageucolab/catalogo-mensajes-usuario.psv}, generado desde los seeds de la BD con
 * {@code AsistenciasUCO-Infra/components/messages/build-message-catalog.py}: si se agrega un codigo en el backend sin su fila en
 * el seed (y sin regenerar el fixture), estas pruebas fallan.</p>
 */
class MensajeUsuarioErrorCodeCatalogoTest {

    private static final String CATALOGO = "/messageucolab/catalogo-mensajes-usuario.psv";
    private static final String DEUDA = "/messageucolab/codigos-error-sin-catalogo.txt";
    private static final Pattern LITERAL_SIN_CODIGO = Pattern.compile(
            "new\\s+(ValidationException|ForbiddenException|ResourceNotFoundException|ConflictException"
                    + "|FeatureUnavailableException|BusinessException|TooManyRequestsException)\\(\\s*\"(?![A-Z][A-Z0-9_]*\"\\s*,)");

    private static Map<String, String> catalogo() throws IOException {
        try (var in = MensajeUsuarioErrorCodeCatalogoTest.class.getResourceAsStream(CATALOGO)) {
            return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines()
                    .filter(l -> !l.isBlank() && !l.startsWith("#"))
                    .map(l -> l.split("\\|", 6))
                    .collect(Collectors.toMap(c -> c[0], c -> c[5], (a, b) -> b, TreeMap::new));
        }
    }

    private static Set<String> deudaConocida() throws IOException {
        try (var in = MensajeUsuarioErrorCodeCatalogoTest.class.getResourceAsStream(DEUDA)) {
            return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines()
                    .map(String::trim)
                    .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    private static int marcadores(final String texto) {
        return texto.split("\\{}", -1).length - 1;
    }

    @Test
    void cada_codigo_de_mensaje_del_backend_esta_en_el_catalogo_de_usuario() throws IOException {
        final Map<String, String> catalogo = catalogo();
        final List<String> faltantes = Stream.of(MensajeUsuarioErrorCode.values())
                .map(MensajeUsuarioErrorCode::messageCode)
                .filter(codigo -> !catalogo.containsKey(codigo))
                .toList();

        assertEquals(List.of(), faltantes, "Agregue la fila al seed de mensajes de la BD y regenere el fixture");
    }

    @Test
    void los_marcadores_del_respaldo_coinciden_con_los_del_catalogo() throws IOException {
        final Map<String, String> catalogo = catalogo();
        for (final MensajeUsuarioErrorCode definicion : MensajeUsuarioErrorCode.values()) {
            assertEquals(marcadores(definicion.defaultMessage()), marcadores(catalogo.get(definicion.messageCode())),
                    definicion.messageCode());
        }
    }

    @Test
    void ningun_codigo_de_error_de_aplicacion_falta_del_catalogo_salvo_la_deuda_registrada() throws Exception {
        final Map<String, String> catalogo = catalogo();
        final Set<String> faltantes = new TreeSet<>();
        for (final JavaClass clase : new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("co.edu.uco.asistenciasuco.application")) {
            if (clase.isEnum() && clase.isAssignableTo(ErrorDefinition.class)) {
                for (final Object constante : clase.reflect().getEnumConstants()) {
                    final String codigo = ((ErrorDefinition) constante).messageCode();
                    if (!catalogo.containsKey(codigo)) {
                        faltantes.add(codigo);
                    }
                }
            }
        }
        final Set<String> deuda = deudaConocida();
        final Set<String> nuevos = new TreeSet<>(faltantes);
        nuevos.removeAll(deuda);
        final Set<String> yaCatalogados = new TreeSet<>(deuda);
        yaCatalogados.removeAll(faltantes);

        assertEquals(Set.of(), nuevos, "Codigos de error nuevos sin fila en el catalogo de mensajes de la BD");
        assertEquals(Set.of(), yaCatalogados, "Ya estan en el catalogo: quitelos de " + DEUDA);
    }

    @Test
    void el_codigo_fuente_no_lanza_errores_de_negocio_con_texto_quemado() throws IOException {
        final List<String> hallazgos = new ArrayList<>();
        try (Stream<Path> archivos = Files.walk(Path.of("src/main/java"))) {
            for (final Path archivo : archivos.filter(p -> p.toString().endsWith(".java")).toList()) {
                final String fuente = Files.readString(archivo, StandardCharsets.UTF_8);
                final Matcher m = LITERAL_SIN_CODIGO.matcher(fuente);
                while (m.find()) {
                    final int linea = fuente.substring(0, m.start()).split("\n", -1).length;
                    hallazgos.add(archivo.getFileName() + ":" + linea);
                }
            }
        }

        assertTrue(hallazgos.isEmpty(), "Use MensajeUsuarioErrorCode (u otra definicion con codigo de catalogo): " + hallazgos);
    }
}
