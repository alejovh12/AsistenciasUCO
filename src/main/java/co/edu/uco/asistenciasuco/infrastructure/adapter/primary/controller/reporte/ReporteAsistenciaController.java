package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.reporte;

import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.GenerarReporteAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.dto.ReporteAsistenciaDTO;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/grupos")
public final class ReporteAsistenciaController {

    private static final String XLSX_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final GenerarReporteAsistenciaInputPort generarReporteAsistenciaInputPort;

    public ReporteAsistenciaController(final GenerarReporteAsistenciaInputPort generarReporteAsistenciaInputPort) {
        this.generarReporteAsistenciaInputPort = Objects.requireNonNull(
                generarReporteAsistenciaInputPort,
                "GenerarReporteAsistenciaInputPort es obligatorio."
        );
    }

    @GetMapping("/{grupoId}/reportes/asistencia-excel")
    public ResponseEntity<byte[]> generarReporteExcel(@PathVariable final UUID grupoId) throws IOException {
        final List<ReporteAsistenciaDTO> rows = generarReporteAsistenciaInputPort.execute(grupoId);
        final byte[] content = buildWorkbook(rows);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(XLSX_MEDIA_TYPE))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("reporte-asistencia-" + grupoId + ".xlsx")
                                .build()
                                .toString()
                )
                .body(content);
    }

    private static byte[] buildWorkbook(final List<ReporteAsistenciaDTO> rows) throws IOException {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            final var sheet = workbook.createSheet("Asistencia");
            writeHeader(sheet.createRow(0));
            int rowIndex = 1;
            for (final ReporteAsistenciaDTO source : rows) {
                writeRow(sheet.createRow(rowIndex), source);
                rowIndex++;
            }
            for (int column = 0; column < 11; column++) {
                sheet.autoSizeColumn(column);
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static void writeHeader(final Row row) {
        final String[] headers = {
                "Codigo grupo",
                "Grupo",
                "Numero sesion",
                "Sesion",
                "Inicio",
                "Fin",
                "Documento",
                "Estudiante",
                "Correo",
                "Estado asistencia",
                "Observacion"
        };
        for (int index = 0; index < headers.length; index++) {
            row.createCell(index).setCellValue(headers[index]);
        }
    }

    private static void writeRow(final Row row, final ReporteAsistenciaDTO source) {
        row.createCell(0).setCellValue(nullToBlank(source.codigoGrupo()));
        row.createCell(1).setCellValue(nullToBlank(source.nombreGrupo()));
        row.createCell(2).setCellValue(source.numeroSesion() == null ? "" : source.numeroSesion().toString());
        row.createCell(3).setCellValue(nullToBlank(source.nombreSesion()));
        row.createCell(4).setCellValue(source.fechaHoraInicio() == null ? "" : source.fechaHoraInicio().toString());
        row.createCell(5).setCellValue(source.fechaHoraFin() == null ? "" : source.fechaHoraFin().toString());
        row.createCell(6).setCellValue(nullToBlank(source.documentoEstudiante()));
        row.createCell(7).setCellValue(nullToBlank(source.nombreEstudiante()));
        row.createCell(8).setCellValue(nullToBlank(source.correoEstudiante()));
        row.createCell(9).setCellValue(estadoPresentacion(source));
        row.createCell(10).setCellValue(nullToBlank(source.razonCausa()));
    }

    private static String nullToBlank(final String value) {
        return value == null ? "" : value;
    }

    private static String estadoPresentacion(final ReporteAsistenciaDTO source) {
        if (source.asistio() == null) {
            return "SIN REGISTRO";
        }
        if (Boolean.TRUE.equals(source.asistio())) {
            return "ASISTIO";
        }
        return source.razonCausa() == null || source.razonCausa().isBlank()
                ? "NO ASISTIO"
                : source.razonCausa();
    }
}
