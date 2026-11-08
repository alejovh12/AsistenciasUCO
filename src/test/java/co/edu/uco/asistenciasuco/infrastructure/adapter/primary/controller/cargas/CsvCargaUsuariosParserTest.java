package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.cargas;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.exception.CargaMasivaErrorCode;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.PerfilDestinoCarga;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvCargaUsuariosParserTest {

    private final CsvCargaUsuariosParser parser = new CsvCargaUsuariosParser();

    @Test
    @DisplayName("D5-P01: Parsea CSV con UTF-8 y BOM opcional con separador punto y coma")
    void parseaCsvConBOMyPuntoYComa() {
        final String csv = "\uFEFFtipo_documento;numero_documento;primer_nombre;primer_apellido;correo;clave_inicial;codigo_tipo_vinculacion;codigo_programa\r\n"
                + "CC;1017123456;Carlos;Gomez;carlos.gomez@uco.edu.co;Clave1234!;TC;ING-SIS\r\n";
        final byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        final List<FilaCsvParseada> filas = parser.parsear(bytes, PerfilDestinoCarga.DOCENTE);

        assertEquals(1, filas.size());
        final FilaCsvParseada fila = filas.get(0);
        assertEquals(2, fila.numeroFila());
        assertEquals("CC", fila.tipoDocumento());
        assertEquals("1017123456", fila.numeroDocumento());
        assertEquals("Carlos", fila.primerNombre());
        assertEquals("Gomez", fila.primerApellido());
        assertEquals("carlos.gomez@uco.edu.co", fila.correo());
        assertEquals("Clave1234!", fila.claveInicial());
        assertEquals("TC", fila.codigoTipoVinculacion());
        assertEquals("ING-SIS", fila.codigoPrograma());
        assertTrue(fila.erroresLocales().isEmpty());
    }

    @Test
    @DisplayName("D5-P02: Parsea CSV sin BOM con separador coma y comillas RFC 4180")
    void parseaCsvSinBOMConComaYComillas() {
        final String csv = "tipo_documento,numero_documento,primer_nombre,segundo_nombre,primer_apellido,segundo_apellido,correo,clave_inicial,codigo_programa\n"
                + "CC,987654321,\"Maria\",\"Isabel\",\"Perez\",\"Lopez\",maria.perez@uco.edu.co,ClaveSegura2026!,MED\n";
        final byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        final List<FilaCsvParseada> filas = parser.parsear(bytes, PerfilDestinoCarga.COORDINADOR);

        assertEquals(1, filas.size());
        final FilaCsvParseada fila = filas.get(0);
        assertEquals(2, fila.numeroFila());
        assertEquals("CC", fila.tipoDocumento());
        assertEquals("987654321", fila.numeroDocumento());
        assertEquals("Maria", fila.primerNombre());
        assertEquals("Isabel", fila.segundoNombre());
        assertEquals("Perez", fila.primerApellido());
        assertEquals("Lopez", fila.segundoApellido());
        assertEquals("maria.perez@uco.edu.co", fila.correo());
        assertEquals("ClaveSegura2026!", fila.claveInicial());
        assertEquals("MED", fila.codigoPrograma());
        assertTrue(fila.erroresLocales().isEmpty());
    }

    @Test
    @DisplayName("D5-P03: Cabeceras en cualquier orden e ignorando mayusculas/minusculas")
    void cabecerasEnCualquierOrdenYCaseInsensitive() {
        final String csv = "CORREO;PRIMER_APELLIDO;PRIMER_NOMBRE;NUMERO_DOCUMENTO;TIPO_DOCUMENTO;CLAVE_INICIAL;CODIGO_FACULTAD\r\n"
                + "decano.salud@uco.edu.co;Zapata;Laura;11223344;CC;ClaveDecano1!;FAC-SALUD\r\n";
        final byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        final List<FilaCsvParseada> filas = parser.parsear(bytes, PerfilDestinoCarga.DECANO);

        assertEquals(1, filas.size());
        final FilaCsvParseada fila = filas.get(0);
        assertEquals("Laura", fila.primerNombre());
        assertEquals("Zapata", fila.primerApellido());
        assertEquals("decano.salud@uco.edu.co", fila.correo());
        assertEquals("FAC-SALUD", fila.codigoFacultad());
        assertTrue(fila.erroresLocales().isEmpty());
    }

    @Test
    @DisplayName("D5-P04: Cabecera desconocida o faltante lanza CMA_004")
    void cabeceraInvalidaLanzaCma004() {
        final String csvFaltante = "primer_nombre;correo\r\nAna;ana@uco.edu.co\r\n";
        final byte[] bytes1 = csvFaltante.getBytes(StandardCharsets.UTF_8);

        final ValidationException ex1 = assertThrows(ValidationException.class,
                () -> parser.parsear(bytes1, PerfilDestinoCarga.DOCENTE));
        assertEquals(CargaMasivaErrorCode.CMA_004.code(), ex1.getCode());

        final String csvDesconocida = "tipo_documento;numero_documento;primer_nombre;primer_apellido;correo;clave_inicial;columna_inexistente\r\n"
                + "CC;123;Ana;Perez;ana@uco.edu.co;Clave123!;XYZ\r\n";
        final byte[] bytes2 = csvDesconocida.getBytes(StandardCharsets.UTF_8);

        final ValidationException ex2 = assertThrows(ValidationException.class,
                () -> parser.parsear(bytes2, PerfilDestinoCarga.DOCENTE));
        assertEquals(CargaMasivaErrorCode.CMA_004.code(), ex2.getCode());
    }

    @Test
    @DisplayName("D5-P05: Lineas vacias se ignoran en el conteo de filas de datos")
    void lineasVaciasIgnoradas() {
        final String csv = "tipo_documento;numero_documento;primer_nombre;primer_apellido;correo;clave_inicial;codigo_tipo_vinculacion;codigo_programa\r\n"
                + "\r\n"
                + "CC;1017123456;Carlos;Gomez;carlos.gomez@uco.edu.co;Clave1234!;TC;ING-SIS\r\n"
                + "   \r\n"
                + "TI;1017654321;Pedro;Perez;pedro.perez@uco.edu.co;Clave1234!;CATEDRA;ING-SIS\r\n"
                + "\r\n";
        final byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        final List<FilaCsvParseada> filas = parser.parsear(bytes, PerfilDestinoCarga.DOCENTE);

        assertEquals(2, filas.size());
        assertEquals(3, filas.get(0).numeroFila()); // linea 3 del archivo
        assertEquals(5, filas.get(1).numeroFila()); // linea 5 del archivo
    }

    @Test
    @DisplayName("D5-P06: Formula injection en columnas ordinarias genera error CMA_011")
    void formulaInjectionGeneraCma011() {
        final String csv = "tipo_documento;numero_documento;primer_nombre;primer_apellido;correo;clave_inicial;codigo_tipo_vinculacion;codigo_programa\r\n"
                + "CC;1017123456;=cmd|' /C calc'!A0;Gomez;carlos.gomez@uco.edu.co;Clave1234!;TC;ING-SIS\r\n"
                + "CC;1017123457;+Carlos;Gomez;carlos2.gomez@uco.edu.co;Clave1234!;TC;ING-SIS\r\n"
                + "CC;1017123458;Carlos;-Gomez;carlos3.gomez@uco.edu.co;Clave1234!;TC;ING-SIS\r\n"
                + "CC;1017123459;Carlos;Gomez;@carlos4.gomez@uco.edu.co;Clave1234!;TC;ING-SIS\r\n";
        final byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        final List<FilaCsvParseada> filas = parser.parsear(bytes, PerfilDestinoCarga.DOCENTE);

        assertEquals(4, filas.size());
        assertTrue(filas.get(0).erroresLocales().stream().anyMatch(e -> "CMA_011".equals(e.codigo())));
        assertTrue(filas.get(1).erroresLocales().stream().anyMatch(e -> "CMA_011".equals(e.codigo())));
        assertTrue(filas.get(2).erroresLocales().stream().anyMatch(e -> "CMA_011".equals(e.codigo())));
        assertTrue(filas.get(3).erroresLocales().stream().anyMatch(e -> "CMA_011".equals(e.codigo())));
    }

    @Test
    @DisplayName("D5-P07: OBS-11 clave_inicial que empieza por '=', '+', '-', '@' es ACEPTADA")
    void claveInicialConPrefijoFormulaAceptada() {
        final String csv = "tipo_documento;numero_documento;primer_nombre;primer_apellido;correo;clave_inicial;codigo_tipo_vinculacion;codigo_programa\r\n"
                + "CC;1017123456;Carlos;Gomez;carlos.gomez@uco.edu.co;@ClaveValida123!;TC;ING-SIS\r\n"
                + "CC;1017123457;Carlos;Gomez;carlos2.gomez@uco.edu.co;+ClaveValida123!;TC;ING-SIS\r\n"
                + "CC;1017123458;Carlos;Gomez;carlos3.gomez@uco.edu.co;-ClaveValida123!;TC;ING-SIS\r\n"
                + "CC;1017123459;Carlos;Gomez;carlos4.gomez@uco.edu.co;=ClaveValida123!;TC;ING-SIS\r\n";
        final byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        final List<FilaCsvParseada> filas = parser.parsear(bytes, PerfilDestinoCarga.DOCENTE);

        assertEquals(4, filas.size());
        for (final FilaCsvParseada f : filas) {
            final boolean tieneCma011 = f.erroresLocales().stream().anyMatch(e -> "CMA_011".equals(e.codigo()));
            assertFalse(tieneCma011, "OBS-11: La clave inicial no debe disparar CMA_011 en fila " + f.numeroFila());
        }
    }

    @Test
    @DisplayName("D5-P08: Archivo que supera el tamano maximo lanza CMA_002")
    void archivoSuperaTamanoMaximoLanzaCma002() {
        final byte[] bytesGrandes = new byte[1048576 + 1];

        final ValidationException ex = assertThrows(ValidationException.class,
                () -> parser.parsear(bytesGrandes, PerfilDestinoCarga.DOCENTE));
        assertEquals(CargaMasivaErrorCode.CMA_002.code(), ex.getCode());
    }

    @Test
    @DisplayName("D5-P09: Archivo que supera 100 filas lanza CMA_001")
    void archivoSupera100FilasLanzaCma001() {
        final StringBuilder sb = new StringBuilder();
        sb.append("tipo_documento;numero_documento;primer_nombre;primer_apellido;correo;clave_inicial;codigo_tipo_vinculacion;codigo_programa\r\n");
        for (int i = 1; i <= 101; i++) {
            sb.append("CC;").append(1000000 + i).append(";Docente;").append(i)
              .append(";docente").append(i).append("@uco.edu.co;ClaveValida123!;TC;ING-SIS\r\n");
        }
        final byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);

        final ValidationException ex = assertThrows(ValidationException.class,
                () -> parser.parsear(bytes, PerfilDestinoCarga.DOCENTE));
        assertEquals(CargaMasivaErrorCode.CMA_001.code(), ex.getCode());
    }

    @Test
    @DisplayName("D5-P10: Numero de documento no entero positivo en rango INT genera error CMA_005")
    void numeroDocumentoInvalidoGeneraCma005() {
        final String csv = "tipo_documento;numero_documento;primer_nombre;primer_apellido;correo;clave_inicial;codigo_tipo_vinculacion;codigo_programa\r\n"
                + "CC;no-es-numero;Carlos;Gomez;carlos.gomez@uco.edu.co;Clave1234!;TC;ING-SIS\r\n"
                + "CC;-500;Carlos;Gomez;carlos2.gomez@uco.edu.co;Clave1234!;TC;ING-SIS\r\n"
                + "CC;99999999999999999;Carlos;Gomez;carlos3.gomez@uco.edu.co;Clave1234!;TC;ING-SIS\r\n";
        final byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        final List<FilaCsvParseada> filas = parser.parsear(bytes, PerfilDestinoCarga.DOCENTE);

        assertEquals(3, filas.size());
        for (final FilaCsvParseada fila : filas) {
            assertTrue(fila.erroresLocales().stream().anyMatch(e -> "CMA_005".equals(e.codigo())),
                    "Debe reportar CMA_005 para documento invalido en fila " + fila.numeroFila());
        }
    }
}
