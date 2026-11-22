package co.edu.uco.asistenciasuco.application.exception.catalog;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * M-05 (auditoria backend 2026-11-22): el texto de respaldo de cada codigo de error debe ser el del catalogo de la BD
 * cuando el codigo existe alli (con tildes). Es lo que ve el usuario cuando el catalogo no responde. Las excepciones son
 * respaldos deliberadamente mas cortos o genericos que el texto parametrizado del catalogo.
 */
class MensajeRespaldoParidadCatalogoTest {

    private static final String CATALOGO = "/messageucolab/catalogo-mensajes-usuario.psv";

    /**
     * Respaldo sin parametros a proposito: el catalogo trae el texto con marcadores {} que arma la BD (limites, estados,
     * fechas) y el respaldo no los conoce; sale generico cuando el catalogo no responde.
     */
    private static final Set<String> RESPALDO_SIN_PARAMETROS_A_PROPOSITO = Set.of(
            "ADS_005", "PRM_004", "PRM_005", "PRM_006", "PRM_007", "PRM_009", "ERR_RESPUESTA_RECLAMO_LONGITUD_INVALIDA",
            "TOM_004", "TOM_012", "RCN_003", "RCN_004", "RCN_006", "RCN_012", "SES_013", "SES_021",
            // el catalogo agrega "y terminar en {}" (dominio institucional)
            "VCE_001",
            // DEC-078: el respaldo pide elegir programa (selector del frontend); el seed de la BD aun trae el texto anterior
            "MQR_008");

    @Test
    void el_respaldo_de_cada_codigo_coincide_con_el_texto_del_catalogo() throws Exception {
        final Map<String, String> catalogo;
        try (var in = MensajeRespaldoParidadCatalogoTest.class.getResourceAsStream(CATALOGO)) {
            catalogo = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines()
                    .filter(l -> !l.isBlank() && !l.startsWith("#"))
                    .map(l -> l.split("\\|", 6))
                    .collect(Collectors.toMap(c -> c[0], c -> c[5], (a, b) -> b, TreeMap::new));
        }
        final Set<String> distintos = new TreeSet<>();
        for (final JavaClass clase : new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("co.edu.uco.asistenciasuco.application")) {
            if (clase.isEnum() && clase.isAssignableTo(ErrorDefinition.class)) {
                for (final Object constante : clase.reflect().getEnumConstants()) {
                    final ErrorDefinition definicion = (ErrorDefinition) constante;
                    final String enCatalogo = catalogo.get(definicion.messageCode());
                    if (enCatalogo != null && !enCatalogo.equals(definicion.defaultMessage())
                            && !RESPALDO_SIN_PARAMETROS_A_PROPOSITO.contains(definicion.messageCode())) {
                        distintos.add(definicion.messageCode() + " => " + definicion.defaultMessage());
                    }
                }
            }
        }
        assertEquals(Set.of(), distintos, "Respaldo distinto al texto del catalogo (corrija tildes y redaccion)");
    }
}
