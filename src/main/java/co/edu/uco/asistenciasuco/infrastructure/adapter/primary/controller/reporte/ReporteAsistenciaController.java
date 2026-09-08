package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.reporte;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Generador de reportes oficiales de asistencia en formato Excel (HU102 / HU066).
 */
@RestController
@RequestMapping("/api/v1/grupos")
public class ReporteAsistenciaController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReporteAsistenciaController.class);

    private final JdbcTemplate jdbcTemplate;

    public ReporteAsistenciaController(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JdbcTemplate es obligatorio.");
    }

    @GetMapping("/{grupoId}/reportes/asistencia-excel")
    public ResponseEntity<byte[]> exportarSabanaExcel(@PathVariable final UUID grupoId) {
        // 1. Obtener metadatos del grupo
        final Map<String, Object> grupoInfo = jdbcTemplate.query(
                "SELECT g.nombre AS grupoNombre, a.codigo AS cursoCodigo, a.nombre AS cursoNombre, " +
                "u.primerNombre + ' ' + u.primerApellido AS docenteNombre " +
                "FROM dbo.Grupo g " +
                "INNER JOIN dbo.Asignatura a ON g.asignatura = a.id " +
                "LEFT JOIN dbo.Docente d ON g.docente = d.id " +
                "LEFT JOIN dbo.Usuario u ON d.usuario = u.id " +
                "WHERE g.id = ?",
                rs -> rs.next() ? Map.of(
                        "grupoNombre", rs.getString("grupoNombre"),
                        "cursoCodigo", rs.getString("cursoCodigo"),
                        "cursoNombre", rs.getString("cursoNombre"),
                        "docenteNombre", Objects.toString(rs.getString("docenteNombre"), "Docente Titular")
                ) : null,
                grupoId
        );

        if (grupoInfo == null) {
            throw new ResourceNotFoundException("El grupo especificado no existe.");
        }

        // 2. Obtener sesiones ordenadas
        final List<Map<String, Object>> sesiones = jdbcTemplate.queryForList(
                "SELECT id, numero, CONVERT(VARCHAR(10), fechaHoraInicio, 120) AS fecha, nombre " +
                "FROM dbo.Sesion WHERE grupo = ? ORDER BY numero ASC",
                grupoId
        );

        // 3. Obtener estudiantes matriculados
        final List<Map<String, Object>> estudiantes = jdbcTemplate.queryForList(
                "SELECT eg.id AS estudianteGrupoId, u.numeroIdentificacion, " +
                "u.primerApellido + ' ' + u.primerNombre AS estudianteNombre " +
                "FROM dbo.EstudianteGrupo eg " +
                "INNER JOIN dbo.Estudiante e ON eg.estudiante = e.id " +
                "INNER JOIN dbo.Usuario u ON e.usuario = u.id " +
                "WHERE eg.grupo = ? ORDER BY u.primerApellido, u.primerNombre",
                grupoId
        );

        // 4. Generar Libro Excel con Apache POI
        try (final Workbook workbook = new XSSFWorkbook(); final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            final Sheet sheet = workbook.createSheet("Sábana de Asistencia");

            // Estilos
            final Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);

            final Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            final CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            final CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            final CellStyle borderStyle = workbook.createCellStyle();
            borderStyle.setBorderBottom(BorderStyle.THIN);
            borderStyle.setBorderTop(BorderStyle.THIN);
            borderStyle.setBorderLeft(BorderStyle.THIN);
            borderStyle.setBorderRight(BorderStyle.THIN);

            // Título y metadatos
            int rowIdx = 0;
            final Row titleRow = sheet.createRow(rowIdx++);
            final Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("UNIVERSIDAD CATÓLICA DE ORIENTE - CONTROL DE ASISTENCIA");
            titleCell.setCellStyle(titleStyle);

            final Row infoRow1 = sheet.createRow(rowIdx++);
            infoRow1.createCell(0).setCellValue("Asignatura: " + grupoInfo.get("cursoCodigo") + " - " + grupoInfo.get("cursoNombre"));
            infoRow1.createCell(4).setCellValue("Grupo: " + grupoInfo.get("grupoNombre"));

            final Row infoRow2 = sheet.createRow(rowIdx++);
            infoRow2.createCell(0).setCellValue("Docente: " + grupoInfo.get("docenteNombre"));
            infoRow2.createCell(4).setCellValue("Total Sesiones: " + sesiones.size());
            rowIdx++; // Espacio en blanco

            // Encabezados de la tabla
            final Row tableHeader = sheet.createRow(rowIdx++);
            int colIdx = 0;
            createCell(tableHeader, colIdx++, "Identificación", headerStyle);
            createCell(tableHeader, colIdx++, "Estudiante", headerStyle);

            for (final Map<String, Object> ses : sesiones) {
                final String headerTitle = "S" + ses.get("numero") + " (" + ses.get("fecha") + ")";
                createCell(tableHeader, colIdx++, headerTitle, headerStyle);
            }

            createCell(tableHeader, colIdx++, "Total Fallas", headerStyle);
            createCell(tableHeader, colIdx++, "% Asistencia", headerStyle);
            createCell(tableHeader, colIdx++, "Estado", headerStyle);

            // Filas de estudiantes
            for (final Map<String, Object> est : estudiantes) {
                final Row dataRow = sheet.createRow(rowIdx++);
                int dataCol = 0;

                createCell(dataRow, dataCol++, Objects.toString(est.get("numeroIdentificacion"), ""), borderStyle);
                createCell(dataRow, dataCol++, Objects.toString(est.get("estudianteNombre"), ""), borderStyle);

                final UUID egId = (UUID) est.get("estudianteGrupoId");
                int inasistencias = 0;

                for (final Map<String, Object> ses : sesiones) {
                    final UUID sesId = (UUID) ses.get("id");
                    final String estadoCodigo = jdbcTemplate.query(
                            "SELECT ea.codigo FROM dbo.Asistencia a " +
                            "INNER JOIN dbo.EstadoAsistencia ea ON a.estado = ea.id " +
                            "WHERE a.estudianteGrupo = ? AND a.sesion = ?",
                            rs -> rs.next() ? rs.getString(1) : null,
                            egId, sesId
                    );

                    final String simbolo;
                    if (estadoCodigo == null) {
                        simbolo = "-";
                    } else if ("IN".equals(estadoCodigo) || "F".equals(estadoCodigo)) {
                        simbolo = "F";
                        inasistencias++;
                    } else if ("JUST".equals(estadoCodigo) || "J".equals(estadoCodigo)) {
                        simbolo = "J";
                    } else if ("TAR".equals(estadoCodigo) || "T".equals(estadoCodigo)) {
                        simbolo = "T";
                    } else {
                        simbolo = "P";
                    }

                    createCell(dataRow, dataCol++, simbolo, borderStyle);
                }

                // Cálculo de estadísticas
                final int totalSes = Math.max(sesiones.size(), 1);
                final double pct = Math.max(0, 100.0 - ((inasistencias * 100.0) / totalSes));
                final String estadoAlumno = inasistencias >= 3 ? "CRÍTICO" : (inasistencias == 2 ? "RIESGO" : "AL DÍA");

                createCell(dataRow, dataCol++, String.valueOf(inasistencias), borderStyle);
                createCell(dataRow, dataCol++, String.format("%.1f%%", pct), borderStyle);
                createCell(dataRow, dataCol++, estadoAlumno, borderStyle);
            }

            // Autoajustar columnas
            for (int i = 0; i < colIdx; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);

            final String filename = "Sabana_Asistencia_" + grupoInfo.get("cursoCodigo") + "_" + grupoInfo.get("grupoNombre") + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());

        } catch (IOException ex) {
            LOGGER.error("Error generando sábana de asistencia en Excel para grupoId={}", grupoId, ex);
            throw new RuntimeException("No fue posible generar el archivo Excel.", ex);
        }
    }

    private void createCell(final Row row, final int column, final String value, final CellStyle style) {
        final Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
}
