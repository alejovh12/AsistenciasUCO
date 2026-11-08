package co.edu.uco.asistenciasuco.architecture;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CULM-001 D.7 (CONTRACT D 8.3, TD-009, HU027/HU033): el registro individual de asistencia
 * {@code POST /api/v1/asistencias} y el SP autonomo {@code usp_registrar_asistencia_estudiante_autonomo} se retiran. La
 * asistencia se registra por el lote ({@code POST /asistencias/lote}), por la toma (B.2) y por token (B.2). No debe quedar
 * ninguna pieza de produccion del registro individual: puerto, caso de uso, DTO, request, validador, mapper, metodo del
 * puerto secundario, matcher de seguridad ni fila del catalogo de acceso.
 */
class RegistroIndividualRetiradoTest {

    private static final String BASE_PACKAGE = "co.edu.uco.asistenciasuco";
    private static final Pattern NOMBRE_RETIRADO = Pattern.compile(
            "(RegistrarAsistencia(InputPort|Interactor|UseCase|UseCaseImpl|DTO|Mapper|Request|RequestValidator|RepositoryDTO|RepositoryMapper))");
    private static final Path SECURITY_CONFIG = Path.of("src", "main", "java", "co", "edu", "uco", "asistenciasuco",
            "infrastructure", "config", "security", "SecurityConfig.java");
    private static final Path ACCESS_CATALOG = Path.of("infra", "security-baseline", "access-catalog.json");

    private static JavaClasses produccion() {
        return new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(BASE_PACKAGE);
    }

    @Test
    void no_quedan_clases_de_produccion_del_registro_individual() {
        final JavaClasses clases = produccion();
        final List<String> sobrantes = clases.stream()
                .filter(clase -> NOMBRE_RETIRADO.matcher(clase.getSimpleName()).matches()
                        || (clase.getPackageName() + ".").contains(".features.asistencia.registrarasistencia."))
                .map(JavaClass::getName)
                .sorted()
                .toList();

        assertTrue(sobrantes.isEmpty(), () -> "Piezas del registro individual aun presentes: " + sobrantes);
    }

    @Test
    void asistencia_conserva_el_lote_y_los_reclamos() {
        // Asercion positiva (R-11): evita un falso verde si el importador no encuentra clases.
        final JavaClasses clases = produccion();
        assertTrue(clases.contain(BASE_PACKAGE + ".infrastructure.adapter.primary.controller.asistencia.AsistenciaController"));
        assertTrue(clases.contain(BASE_PACKAGE
                + ".application.features.asistencia.registrarasistenciassesion.primaryports.RegistrarAsistenciasSesionInputPort"));
    }

    @Test
    void el_puerto_secundario_ya_no_registra_asistencia_individual() {
        final List<String> metodos = Arrays.stream(AsistenciaRepositoryPort.class.getMethods())
                .map(java.lang.reflect.Method::getName)
                .filter("registrarAsistencia"::equals)
                .toList();

        assertTrue(metodos.isEmpty(), "AsistenciaRepositoryPort.registrarAsistencia debe desaparecer");
    }

    @Test
    void ningun_handler_post_responde_en_la_raiz_de_asistencias() throws Exception {
        final Class<?> controlador = Class.forName(BASE_PACKAGE + ".infrastructure.adapter.primary.controller.asistencia.AsistenciaController");
        final List<String> raiz = Arrays.stream(controlador.getDeclaredMethods())
                .map(metodo -> metodo.getAnnotation(org.springframework.web.bind.annotation.PostMapping.class))
                .filter(java.util.Objects::nonNull)
                .filter(mapping -> mapping.value().length == 0 && mapping.path().length == 0)
                .map(Object::toString)
                .toList();

        assertTrue(raiz.isEmpty(), "POST /api/v1/asistencias debe responder 404: sin handler en la raiz");
    }

    @Test
    void security_config_y_catalogo_de_acceso_no_declaran_la_ruta_retirada() throws IOException {
        final String seguridad = Files.readString(SECURITY_CONFIG);
        assertFalse(seguridad.contains("HttpMethod.POST, \"/api/v1/asistencias\")"),
                "SecurityConfig aun declara el matcher POST /api/v1/asistencias");
        assertTrue(seguridad.contains("\"/api/v1/asistencias/lote\""), "el matcher del lote debe conservarse");

        final String catalogo = Files.readString(ACCESS_CATALOG);
        assertFalse(Pattern.compile("\"method\":\s*\"POST\",\s*\"path\":\s*\"/api/v1/asistencias\"").matcher(catalogo).find(),
                "access-catalog.json aun declara POST /api/v1/asistencias");
    }
}
