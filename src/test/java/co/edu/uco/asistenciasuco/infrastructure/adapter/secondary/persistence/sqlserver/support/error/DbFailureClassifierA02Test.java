package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A-02 (auditoria backend 2026-11-22): los codigos de negocio que emiten los SP deben tener clasificacion HTTP; sin ella
 * salian como 500 ERR_DB_UNCLASSIFIED sin pista para el usuario. La paridad se comprueba contra el fixture del catalogo de
 * mensajes (generado desde los seeds de la BD).
 */
class DbFailureClassifierA02Test {

    private static final String CATALOGO = "/messageucolab/catalogo-mensajes-usuario.psv";
    /** Codigo de la BD con forma XXX_nnn (los VAL_APP_*, ACC_APP_* y ERR_* nacen en la aplicacion, no en un SP). */
    private static final Pattern CODIGO_DE_SP = Pattern.compile("^[A-Z]{2,5}_\\d{3}$");

    /**
     * Codigos que a proposito no tienen clasificacion de negocio en el clasificador de comandos:
     * GEN_001, SYS_001 y VAL_007 son genericos o dependen de la operacion (quedan tecnicos sin ella);
     * (NTF_001 y NTF_003 ya se clasifican: F4 los emiten solo los SP del outbox que el backend conserva);
     * EST_002, GRUP_002 y SES_002 son codigos historicos que los SP ya no lanzan (los reemplazan ERR_MATRICULA_DUPLICADA,
     * ERR_CUPO_SUPERADO y AQR_*); MQR_002/003, AQR_011 e IAM_001 los resuelve la aplicacion (red, limite de intentos, cuenta);
     * FES_010, ALR_002 y VCE_009 son avisos que no cortan la operacion; HOR_010..014 son informativos del horario.
     */
    private static final Set<String> SIN_CLASIFICAR_A_PROPOSITO = Set.of(
            "GEN_001", "SYS_001", "VAL_007",
            "EST_002", "GRUP_002", "SES_002", "MQR_002", "MQR_003", "AQR_011", "IAM_001",
            "FES_010", "ALR_002", "VCE_009", "HOR_010", "HOR_011", "HOR_012", "HOR_013", "HOR_014");

    @ParameterizedTest
    @CsvSource({
            "GRUP_001, ResourceNotFoundException",
            "GRUP_003, ConflictException",
            "SES_006, ConflictException",
            "EST_003, ConflictException",
            "INST_001, ValidationException",
            "KPI_001, ForbiddenException",
            "KPI_003, ForbiddenException",
            "KPI_004, ForbiddenException",
            "VCE_003, ConflictException",
            "VCE_004, ResourceNotFoundException",
            "VCE_005, ConflictException",
            "VCE_007, ResourceNotFoundException",
            "ACT_002, ConflictException",
            "MQR_001, ResourceNotFoundException",
            "MQR_007, ValidationException",
            "FES_001, ValidationException",
            "PAG_001, ValidationException"
    })
    void el_codigo_de_negocio_se_traduce_al_http_esperado_con_su_propio_codigo(final String codigo, final String tipo) {
        final RuntimeException traducida = DbExceptionTranslator.translateDbCode(codigo, null).orElseThrow();

        assertEquals(tipo, traducida.getClass().getSimpleName());
        assertTrue(traducida instanceof ConflictException || traducida instanceof ResourceNotFoundException
                || traducida instanceof ValidationException || traducida instanceof ForbiddenException);
    }

    @Test
    void los_codigos_kpi_conservan_su_codigo_publico_que_usa_el_frontend() {
        for (final String codigo : List.of("KPI_001", "KPI_003", "KPI_004")) {
            final ForbiddenException e = assertInstanceOf(ForbiddenException.class,
                    DbExceptionTranslator.translateDbCode(codigo, null).orElseThrow());
            assertEquals(codigo, e.getCode());
        }
    }

    @ParameterizedTest
    @CsvSource({"CORR_001", "KPI_002"})
    void los_codigos_tecnicos_quedan_clasificados_como_inconsistencia_de_catalogo_no_por_defecto(final String codigo) {
        assertEquals(DatabaseErrorCode.ERR_DB_CATALOG_INCONSISTENT,
                DbFailureClassifier.classify(null, "DBCODE=" + codigo + "|x", null));
    }

    @Test
    void todo_codigo_de_error_de_negocio_del_catalogo_tiene_clasificacion() throws IOException {
        final List<String> sinClasificar = new ArrayList<>();
        try (var in = DbFailureClassifierA02Test.class.getResourceAsStream(CATALOGO);
             var lector = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            lector.lines().filter(l -> !l.isBlank() && !l.startsWith("#")).map(l -> l.split("\\|", 6))
                    .filter(c -> "Error".equals(c[3]) && CODIGO_DE_SP.matcher(c[0]).matches())
                    .filter(c -> !SIN_CLASIFICAR_A_PROPOSITO.contains(c[0]))
                    .forEach(c -> {
                        if (DatabaseErrorCode.ERR_DB_UNCLASSIFIED.equals(
                                DbFailureClassifier.classify(null, "DBCODE=" + c[0] + "|x", null))) {
                            sinClasificar.add(c[0]);
                        }
                    });
        }

        assertEquals(List.of(), sinClasificar,
                "Codigos de error del catalogo sin clasificar en DbFailureClassifier (caerian como 500 generico)");
    }

    @Test
    void la_lista_de_excepciones_no_oculta_codigos_que_ya_estan_clasificados() {
        for (final String codigo : SIN_CLASIFICAR_A_PROPOSITO) {
            assertEquals(DatabaseErrorCode.ERR_DB_UNCLASSIFIED,
                    DbFailureClassifier.classify(null, "DBCODE=" + codigo + "|x", null), codigo);
        }
        assertNotEquals(0, SIN_CLASIFICAR_A_PROPOSITO.size());
    }
}
