package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.reporte;

import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.GenerarReporteAsistenciaExcelInputPort;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.dto.GenerarReporteAsistenciaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/** Descarga del reporte de asistencia del grupo; la generacion del libro la hace el adaptador detras de {@code ReporteExcelPort}. */
@RestController
@RequestMapping("/api/v1/grupos")
public final class ReporteAsistenciaController {

    private static final String XLSX_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final GenerarReporteAsistenciaExcelInputPort generarReporteAsistenciaExcelInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ReporteAsistenciaController(
            final GenerarReporteAsistenciaExcelInputPort generarReporteAsistenciaExcelInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.generarReporteAsistenciaExcelInputPort = Objects.requireNonNull(
                generarReporteAsistenciaExcelInputPort,
                "GenerarReporteAsistenciaExcelInputPort es obligatorio."
        );
        this.authenticatedUserResolver = Objects.requireNonNull(
                authenticatedUserResolver,
                "AuthenticatedUserResolver es obligatorio."
        );
    }

    @GetMapping("/{grupoId}/reportes/asistencia-excel")
    public ResponseEntity<byte[]> generarReporteExcel(@PathVariable final UUID grupoId) {
        final byte[] content = generarReporteAsistenciaExcelInputPort.execute(
                new GenerarReporteAsistenciaDTO(grupoId, authenticatedUserResolver.requireAuthenticatedUserId())
        );

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
}
