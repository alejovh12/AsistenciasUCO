package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.report;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.report.ReporteAsistenciaRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PoiReporteExcelAdapterTest {

    private static final String[] ENCABEZADOS = {"Código del grupo", "Grupo", "Número de sesión", "Sesión", "Inicio (America/Bogota)", "Fin (America/Bogota)",
            "Documento", "Estudiante", "Correo", "Estado de asistencia", "Observación"};

    private final PoiReporteExcelAdapter adaptador = new PoiReporteExcelAdapter(codigo -> Optional.empty(), 100);

    @Test
    void exporta_columnas_filas_y_estados_de_asistencia_con_tildes() throws Exception {
        final byte[] libro = adaptador.generarReporteAsistencia(List.of(
                fila(Boolean.TRUE, null),
                fila(Boolean.FALSE, "Excusa médica"),
                fila(null, null),
                fila(Boolean.FALSE, null)));

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(libro))) {
            final var sheet = workbook.getSheet("Asistencia");
            assertNotNull(sheet);
            assertEquals(5, sheet.getPhysicalNumberOfRows());
            assertArrayEquals(ENCABEZADOS, IntStream.range(0, 11)
                    .mapToObj(index -> sheet.getRow(0).getCell(index).getStringCellValue()).toArray(String[]::new));
            assertEquals("G01", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("1", sheet.getRow(1).getCell(2).getStringCellValue());
            assertEquals("ASISTIÓ", sheet.getRow(1).getCell(9).getStringCellValue());
            assertEquals("Excusa médica", sheet.getRow(2).getCell(9).getStringCellValue());
            assertEquals("SIN REGISTRO", sheet.getRow(3).getCell(9).getStringCellValue());
            assertEquals("NO ASISTIÓ", sheet.getRow(4).getCell(9).getStringCellValue());
            assertEquals("", sheet.getRow(3).getCell(10).getStringCellValue());
        }
    }

    @Test
    void una_sesion_cancelada_sale_como_cancelada_y_no_como_sin_registro() throws Exception {
        final byte[] libro = adaptador.generarReporteAsistencia(List.of(filaCancelada(null), filaCancelada(Boolean.FALSE)));

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(libro))) {
            final var sheet = workbook.getSheet("Asistencia");
            assertEquals("CANCELADA", sheet.getRow(1).getCell(9).getStringCellValue());
            assertEquals("CANCELADA", sheet.getRow(2).getCell(9).getStringCellValue());
        }
    }

    @Test
    void el_rotulo_cancelada_sale_del_catalogo_cuando_existe() throws IOException {
        final MessageCatalogPort catalogo = mock(MessageCatalogPort.class);
        when(catalogo.findUserMessage("RPT_ASISTENCIA_ESTADO_CANCELADA")).thenReturn(Optional.of("SESIÓN CANCELADA"));
        final var conCatalogo = new PoiReporteExcelAdapter(catalogo, 10);
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(conCatalogo.generarReporteAsistencia(List.of(filaCancelada(null)))))) {
            assertEquals("SESIÓN CANCELADA", workbook.getSheet("Asistencia").getRow(1).getCell(9).getStringCellValue());
        }
    }

    @Test
    void inicio_y_fin_se_escriben_en_hora_institucional_con_formato_dia_mes_anio() throws Exception {
        // La base guarda UTC: 13:00Z es 08:00 en Bogota (UTC-5, sin horario de verano).
        final var fila = new ReporteAsistenciaRow("G01", "Grupo 01", 1, "Sesión 1",
                LocalDateTime.of(2026, 11, 23, 13, 0), LocalDateTime.of(2026, 11, 23, 15, 30),
                "12345678", "Persona", "persona@example.com", Boolean.TRUE, null);
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(adaptador.generarReporteAsistencia(List.of(fila))))) {
            final var sheet = workbook.getSheet("Asistencia");
            assertEquals("23/11/2026 08:00", sheet.getRow(1).getCell(4).getStringCellValue());
            assertEquals("23/11/2026 10:30", sheet.getRow(1).getCell(5).getStringCellValue());
        }
    }

    @Test
    void la_zona_configurada_define_las_horas_y_se_indica_en_el_encabezado() throws Exception {
        final var madrid = new PoiReporteExcelAdapter(codigo -> Optional.empty(), 10, ZoneId.of("Europe/Madrid"));
        final var fila = new ReporteAsistenciaRow("G01", "Grupo 01", 1, "Sesión 1",
                LocalDateTime.of(2026, 7, 1, 13, 0), LocalDateTime.of(2026, 7, 1, 14, 0),
                "1", "P", "p@example.com", Boolean.TRUE, null);
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(madrid.generarReporteAsistencia(List.of(fila))))) {
            final var sheet = workbook.getSheet("Asistencia");
            assertEquals("Inicio (Europe/Madrid)", sheet.getRow(0).getCell(4).getStringCellValue());
            assertEquals("Fin (Europe/Madrid)", sheet.getRow(0).getCell(5).getStringCellValue());
            assertEquals("01/07/2026 15:00", sheet.getRow(1).getCell(4).getStringCellValue());
        }
    }

    @Test
    void la_zona_es_obligatoria() {
        assertThrows(NullPointerException.class, () -> new PoiReporteExcelAdapter(codigo -> Optional.empty(), 10, (ZoneId) null));
    }

    @Test
    void un_reporte_vacio_solo_trae_el_encabezado() throws Exception {
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(adaptador.generarReporteAsistencia(List.of())))) {
            assertEquals(1, workbook.getSheet("Asistencia").getPhysicalNumberOfRows());
        }
    }

    @Test
    void los_valores_nulos_salen_como_celdas_vacias_y_el_estado_sin_registro() throws Exception {
        final var nulos = new ReporteAsistenciaRow(null, null, null, null, null, null, null, null, null, null, null);
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(adaptador.generarReporteAsistencia(List.of(nulos))))) {
            final var sheet = workbook.getSheet("Asistencia");
            assertEquals(11, sheet.getRow(1).getPhysicalNumberOfCells());
            for (int columna = 0; columna < 11; columna++) {
                assertEquals(columna == 9 ? "SIN REGISTRO" : "", sheet.getRow(1).getCell(columna).getStringCellValue());
            }
        }
    }

    @Test
    void los_rotulos_salen_del_catalogo_cuando_existen() throws IOException {
        final MessageCatalogPort catalogo = mock(MessageCatalogPort.class);
        when(catalogo.findUserMessage("RPT_ASISTENCIA_GRUPO")).thenReturn(Optional.of("Grupo académico"));
        when(catalogo.findUserMessage("RPT_ASISTENCIA_ESTADO_SIN_REGISTRO")).thenReturn(Optional.of(" "));
        when(catalogo.findUserMessage("RPT_ASISTENCIA_CODIGO_GRUPO")).thenReturn(Optional.empty());
        final var conCatalogo = new PoiReporteExcelAdapter(catalogo, 10);

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(
                conCatalogo.generarReporteAsistencia(List.of(fila(null, null)))))) {
            final var sheet = workbook.getSheet("Asistencia");
            assertEquals("Grupo académico", sheet.getRow(0).getCell(1).getStringCellValue());
            assertEquals("Código del grupo", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("SIN REGISTRO", sheet.getRow(1).getCell(9).getStringCellValue());
        }
    }

    @Test
    void sin_catalogo_usa_los_rotulos_de_respaldo() throws IOException {
        final var sinCatalogo = new PoiReporteExcelAdapter((MessageCatalogPort) null, 10);
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(sinCatalogo.generarReporteAsistencia(List.of())))) {
            assertEquals("Observación", workbook.getSheet("Asistencia").getRow(0).getCell(10).getStringCellValue());
        }
    }

    @Test
    void un_reporte_con_mas_filas_que_el_tope_se_rechaza_con_error_de_validacion() {
        final var corto = new PoiReporteExcelAdapter(codigo -> Optional.empty(), 2);
        final var filas = List.of(fila(Boolean.TRUE, null), fila(Boolean.TRUE, null), fila(Boolean.TRUE, null));

        final ValidationException exception = assertThrows(ValidationException.class, () -> corto.generarReporteAsistencia(filas));

        assertEquals("VALIDATION_ERROR", exception.getCode());
        assertTrue(corto.generarReporteAsistencia(filas.subList(0, 2)).length > 0);
    }

    @Test
    void el_tope_debe_ser_positivo() {
        assertThrows(IllegalArgumentException.class, () -> new PoiReporteExcelAdapter(codigo -> Optional.empty(), 0));
    }

    private static ReporteAsistenciaRow fila(final Boolean asistio, final String razon) {
        return new ReporteAsistenciaRow("G01", "Grupo 01", 1, "Sesión 1",
                LocalDateTime.of(2026, 1, 1, 8, 0), LocalDateTime.of(2026, 1, 1, 9, 0),
                "12345678", "Persona", "persona@example.com", asistio, razon);
    }

    private static ReporteAsistenciaRow filaCancelada(final Boolean asistio) {
        return new ReporteAsistenciaRow("G01", "Grupo 01", 1, "Sesión 1",
                LocalDateTime.of(2026, 1, 1, 8, 0), LocalDateTime.of(2026, 1, 1, 9, 0),
                "12345678", "Persona", "persona@example.com", asistio, null, true);
    }
}
