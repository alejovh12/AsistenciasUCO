package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.reporte;

import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.GenerarReporteAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.dto.ReporteAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.dto.GenerarReporteAsistenciaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReporteAsistenciaControllerTest {

    @Test
    void exportsWorkbookWithExpectedColumnsRowsAndAttendanceStates() throws Exception {
        final UUID grupoId = UUID.randomUUID();
        final UUID actorId = UUID.randomUUID();
        final AtomicReference<GenerarReporteAsistenciaDTO> request = new AtomicReference<>();
        final GenerarReporteAsistenciaInputPort port = input -> {
            request.set(input);
            return List.of(
                    row(Boolean.TRUE, null),
                    row(Boolean.FALSE, "Excusa médica"),
                    row(null, null),
                    row(Boolean.FALSE, null)
            );
        };

        final AuthenticatedUserResolver resolver = () -> actorId;
        final ResponseEntity<byte[]> response = new ReporteAsistenciaController(port, resolver).generarReporteExcel(grupoId);

        assertEquals(grupoId, request.get().grupoId());
        assertEquals(actorId, request.get().usuarioEjecutor());
        assertEquals(200, response.getStatusCode().value());
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                response.getHeaders().getContentType().toString());
        assertTrue(response.getHeaders().getContentDisposition().getFilename().contains(grupoId.toString()));
        assertNotNull(response.getBody());
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getBody()))) {
            final var sheet = workbook.getSheet("Asistencia");
            assertNotNull(sheet);
            assertEquals(5, sheet.getPhysicalNumberOfRows());
            assertArrayEquals(new String[] {"Codigo grupo", "Grupo", "Numero sesion", "Sesion", "Inicio", "Fin",
                    "Documento", "Estudiante", "Correo", "Estado asistencia", "Observacion"},
                    java.util.stream.IntStream.range(0, 11)
                            .mapToObj(index -> sheet.getRow(0).getCell(index).getStringCellValue())
                            .toArray(String[]::new));
            assertEquals("G01", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("1", sheet.getRow(1).getCell(2).getStringCellValue());
            assertEquals("ASISTIO", sheet.getRow(1).getCell(9).getStringCellValue());
            assertEquals("Excusa médica", sheet.getRow(2).getCell(9).getStringCellValue());
            assertEquals("SIN REGISTRO", sheet.getRow(3).getCell(9).getStringCellValue());
            assertEquals("NO ASISTIO", sheet.getRow(4).getCell(9).getStringCellValue());
            assertEquals("", sheet.getRow(3).getCell(10).getStringCellValue());
        }
    }

    @Test
    void exportsHeaderForEmptyReport() throws Exception {
        final ResponseEntity<byte[]> response = new ReporteAsistenciaController(input -> List.of(), UUID::randomUUID    )
                .generarReporteExcel(UUID.randomUUID());
        assertNotNull(response.getBody());
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getBody()))) {
            assertEquals(1, workbook.getSheet("Asistencia").getPhysicalNumberOfRows());
        }
    }

    @Test
    void exportsValidWorkbookWithNullValuesAndAllElevenColumns() throws Exception {
        final UUID grupoId = UUID.randomUUID();
        final ReporteAsistenciaDTO nullValues = new ReporteAsistenciaDTO(
                null, null, null, null, null, null, null, null, null, null, null);
        final ResponseEntity<byte[]> response = new ReporteAsistenciaController(
                input -> List.of(nullValues), UUID::randomUUID).generarReporteExcel(grupoId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                response.getHeaders().getContentType().toString());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getBody()))) {
            final var sheet = workbook.getSheet("Asistencia");
            assertNotNull(sheet);
            assertEquals(2, sheet.getPhysicalNumberOfRows());
            assertEquals(11, sheet.getRow(0).getPhysicalNumberOfCells());
            assertEquals(11, sheet.getRow(1).getPhysicalNumberOfCells());
            assertArrayEquals(new String[] {"Codigo grupo", "Grupo", "Numero sesion", "Sesion", "Inicio", "Fin",
                    "Documento", "Estudiante", "Correo", "Estado asistencia", "Observacion"},
                    java.util.stream.IntStream.range(0, 11)
                            .mapToObj(index -> sheet.getRow(0).getCell(index).getStringCellValue())
                            .toArray(String[]::new));
            for (int column = 0; column < 11; column++) {
                assertNotNull(sheet.getRow(1).getCell(column));
                assertEquals(column == 9 ? "SIN REGISTRO" : "",
                        sheet.getRow(1).getCell(column).getStringCellValue());
            }
        }
    }

    private static ReporteAsistenciaDTO row(final Boolean asistio, final String reason) {
        return new ReporteAsistenciaDTO("G01", "Grupo 01", 1, "Sesión 1",
                LocalDateTime.of(2026, 1, 1, 8, 0), LocalDateTime.of(2026, 1, 1, 9, 0),
                "12345678", "Persona", "persona@example.com", asistio, reason);
    }
}
