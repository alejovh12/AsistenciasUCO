package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.reporte;

import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.GenerarReporteAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.dto.ReporteAsistenciaDTO;
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
        final AtomicReference<UUID> requestedGroup = new AtomicReference<>();
        final GenerarReporteAsistenciaInputPort port = id -> {
            requestedGroup.set(id);
            return List.of(
                    row(Boolean.TRUE, null),
                    row(Boolean.FALSE, "Excusa médica"),
                    row(null, null),
                    row(Boolean.FALSE, null)
            );
        };

        final ResponseEntity<byte[]> response = new ReporteAsistenciaController(port).generarReporteExcel(grupoId);

        assertEquals(grupoId, requestedGroup.get());
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
        final ResponseEntity<byte[]> response = new ReporteAsistenciaController(id -> List.of())
                .generarReporteExcel(UUID.randomUUID());
        assertNotNull(response.getBody());
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getBody()))) {
            assertEquals(1, workbook.getSheet("Asistencia").getPhysicalNumberOfRows());
        }
    }

    private static ReporteAsistenciaDTO row(final Boolean asistio, final String reason) {
        return new ReporteAsistenciaDTO("G01", "Grupo 01", 1, "Sesión 1",
                LocalDateTime.of(2026, 1, 1, 8, 0), LocalDateTime.of(2026, 1, 1, 9, 0),
                "12345678", "Persona", "persona@example.com", asistio, reason);
    }
}
