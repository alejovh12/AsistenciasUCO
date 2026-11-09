package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.cargas;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.ErrorFilaCargaDomain;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.FilaCsvParseada;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.PerfilDestinoCarga;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Validaciones locales por fila, cabeceras por perfil y casos limite de RFC 4180 del lector CSV de carga masiva. */
class CsvCargaUsuariosParserValidacionesTest {

    private static final String CABECERA_DOCENTE = "tipo_documento;numero_documento;primer_nombre;primer_apellido;correo;clave_inicial;codigo_tipo_vinculacion\r\n";
    private static final String CABECERA_COORDINADOR = "tipo_documento;numero_documento;primer_nombre;primer_apellido;correo;clave_inicial;codigo_programa\r\n";
    private static final String CABECERA_DECANO = "tipo_documento;numero_documento;primer_nombre;primer_apellido;correo;clave_inicial;codigo_facultad\r\n";

    private final CsvCargaUsuariosParser parser = new CsvCargaUsuariosParser();

    private List<FilaCsvParseada> parsear(final String csv, final PerfilDestinoCarga perfil) {
        return parser.parsear(csv.getBytes(StandardCharsets.UTF_8), perfil);
    }

    private static boolean tiene(final FilaCsvParseada fila, final String campo, final String codigo) {
        return fila.erroresLocales().stream().anyMatch((ErrorFilaCargaDomain e) -> e.campo().equals(campo) && e.codigo().equals(codigo));
    }

    @Test
    void un_archivo_nulo_o_vacio_es_cma_013() {
        assertEquals("CMA_013", assertThrows(ValidationException.class, () -> parser.parsear(null, PerfilDestinoCarga.DOCENTE)).getCode());
        assertEquals("CMA_013", assertThrows(ValidationException.class, () -> parser.parsear(new byte[0], PerfilDestinoCarga.DOCENTE)).getCode());
    }

    @Test
    void bytes_que_no_son_utf8_valido_son_cma_013() {
        final byte[] invalido = {(byte) 0xC3, (byte) 0x28, (byte) 0xFF, (byte) 0xFE};
        assertEquals("CMA_013", assertThrows(ValidationException.class, () -> parser.parsear(invalido, PerfilDestinoCarga.DOCENTE)).getCode());
    }

    @Test
    void un_archivo_solo_de_lineas_vacias_es_cma_004() {
        assertEquals("CMA_004", assertThrows(ValidationException.class, () -> parsear(";;;\r\n   \r\n\r\n", PerfilDestinoCarga.DOCENTE)).getCode());
    }

    @Test
    void cabecera_sin_columna_obligatoria_o_con_columna_ajena_al_perfil_es_cma_004() {
        // coordinador sin codigo_programa
        assertEquals("CMA_004", assertThrows(ValidationException.class, () -> parsear(
                "tipo_documento;numero_documento;primer_nombre;primer_apellido;correo;clave_inicial\r\nCC;1;A;B;a@uco.edu.co;x\r\n",
                PerfilDestinoCarga.COORDINADOR)).getCode());
        // decano con una columna que solo existe para docentes
        assertEquals("CMA_004", assertThrows(ValidationException.class, () -> parsear(
                CABECERA_DECANO.replace("codigo_facultad", "codigo_tipo_vinculacion") + "CC;1;A;B;a@uco.edu.co;x;TC\r\n",
                PerfilDestinoCarga.DECANO)).getCode());
    }

    @Test
    void los_campos_obligatorios_vacios_generan_un_error_por_campo() {
        final List<FilaCsvParseada> filas = parsear(CABECERA_DOCENTE + ";;;;;;\r\n;  ;;;;x;\r\n", PerfilDestinoCarga.DOCENTE);

        final FilaCsvParseada vacia = filas.getFirst();
        assertTrue(tiene(vacia, "numero_documento", "CMA_005"));
        assertTrue(tiene(vacia, "primer_nombre", "VAL_001"));
        assertTrue(tiene(vacia, "primer_apellido", "VAL_001"));
        assertTrue(tiene(vacia, "correo", "VAL_001"));
        assertTrue(tiene(vacia, "codigo_tipo_vinculacion", "VAL_001"));
    }

    @Test
    void un_correo_sin_arroba_y_un_documento_fuera_de_rango_se_rechazan() {
        final List<FilaCsvParseada> filas = parsear(CABECERA_DOCENTE
                + "CC;1017123456;Ana;Perez;sin-arroba;;TC\r\n"
                + "CC;99999999999;Ana;Perez;ana@uco.edu.co;;TC\r\n"
                + "CC;-5;Ana;Perez;ana2@uco.edu.co;;TC\r\n"
                + "CC;12ab;Ana;Perez;ana3@uco.edu.co;;TC\r\n", PerfilDestinoCarga.DOCENTE);

        assertTrue(tiene(filas.get(0), "correo", "VCE_001"));
        assertTrue(tiene(filas.get(1), "numero_documento", "CMA_005"));
        assertTrue(tiene(filas.get(2), "numero_documento", "CMA_005"));
        assertTrue(tiene(filas.get(3), "numero_documento", "CMA_005"));
    }

    @Test
    void una_clave_inicial_que_incumple_la_politica_se_reporta_sin_exponerla() {
        final List<FilaCsvParseada> filas = parsear(CABECERA_DOCENTE + "CC;1017123456;Ana;Perez;ana@uco.edu.co;corta;TC\r\n", PerfilDestinoCarga.DOCENTE);

        final FilaCsvParseada fila = filas.getFirst();
        assertTrue(tiene(fila, "clave_inicial", "ERR_PASSWORD_POLITICA_INVALIDA"));
        assertTrue(fila.erroresLocales().stream().noneMatch(e -> e.mensaje().contains("corta")));
    }

    @Test
    void el_coordinador_exige_codigo_de_programa() {
        final List<FilaCsvParseada> filas = parsear(CABECERA_COORDINADOR + "CC;1017123456;Ana;Perez;ana@uco.edu.co;;\r\n", PerfilDestinoCarga.COORDINADOR);
        assertTrue(tiene(filas.getFirst(), "codigo_programa", "VAL_001"));
    }

    @Test
    void documento_y_correo_repetidos_en_el_archivo_son_cma_010_con_la_linea_original() {
        final List<FilaCsvParseada> filas = parsear(CABECERA_DECANO
                + "CC;1017123456;Ana;Perez;ana@uco.edu.co;;FAC-ING\r\n"
                + "cc;1017123456;Ana;Perez;otro@uco.edu.co;;FAC-ING\r\n"
                + "CC;1017999999;Luis;Ruiz;ANA@uco.edu.co;;FAC-ING\r\n", PerfilDestinoCarga.DECANO);

        assertTrue(filas.get(0).erroresLocales().isEmpty());
        assertTrue(tiene(filas.get(1), "numero_documento", "CMA_010"));
        assertTrue(filas.get(1).erroresLocales().stream().anyMatch(e -> e.mensaje().contains("línea 2")));
        assertTrue(tiene(filas.get(2), "correo", "CMA_010"));
    }

    @Test
    void comillas_escapadas_saltos_de_linea_dentro_de_comillas_y_ultima_fila_sin_salto_final() {
        final List<FilaCsvParseada> filas = parsear(CABECERA_DECANO
                + "CC;1017123456;\"Ana \"\"La Grande\"\"\";Perez;ana@uco.edu.co;;FAC-ING\n"
                + "CC;1017123457;\"Luis\nMaria\";Ruiz;luis@uco.edu.co;;FAC-ING", PerfilDestinoCarga.DECANO);

        assertEquals(2, filas.size());
        assertEquals("Ana \"La Grande\"", filas.get(0).primerNombre());
        assertEquals("Luis\nMaria", filas.get(1).primerNombre());
        assertEquals("FAC-ING", filas.get(1).codigoFacultad());
    }

    @Test
    void el_separador_se_detecta_ignorando_las_comas_dentro_de_comillas_y_las_filas_cortas_rellenan_con_vacio() {
        final List<FilaCsvParseada> filas = parsear(
                "tipo_documento,numero_documento,primer_nombre,primer_apellido,correo,clave_inicial,codigo_facultad\r\n"
                        + "CC,1017123456,\"Ana, la primera\",Perez,ana@uco.edu.co\r\n", PerfilDestinoCarga.DECANO);

        assertEquals("Ana, la primera", filas.getFirst().primerNombre());
        assertEquals("", filas.getFirst().claveInicial());
        assertEquals("", filas.getFirst().codigoFacultad());
    }

    @Test
    void los_prefijos_de_formula_en_cualquier_columna_menos_la_clave_son_cma_011() {
        final List<FilaCsvParseada> filas = parsear(CABECERA_DECANO + "=CC;+123;-Ana;@Perez;ana@uco.edu.co;=Clave1234!;FAC-ING\r\n", PerfilDestinoCarga.DECANO);

        final FilaCsvParseada fila = filas.getFirst();
        assertTrue(tiene(fila, "tipo_documento", "CMA_011"));
        assertTrue(tiene(fila, "numero_documento", "CMA_011"));
        assertTrue(tiene(fila, "primer_nombre", "CMA_011"));
        assertTrue(tiene(fila, "primer_apellido", "CMA_011"));
        assertTrue(fila.erroresLocales().stream().noneMatch(e -> e.campo().equals("clave_inicial") && e.codigo().equals("CMA_011")));
    }
}
