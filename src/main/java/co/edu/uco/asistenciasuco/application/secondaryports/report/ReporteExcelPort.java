package co.edu.uco.asistenciasuco.application.secondaryports.report;

import java.util.List;

/**
 * Puerto secundario que serializa el reporte de asistencia de un grupo a un libro de Excel (.xlsx).
 *
 * <p>La aplicacion no conoce la libreria de hojas de calculo ni el formato de las celdas. Un reporte con mas filas que el tope
 * configurado se rechaza con un error de validacion en lugar de armarse completo en memoria.</p>
 */
public interface ReporteExcelPort {

    byte[] generarReporteAsistencia(List<ReporteAsistenciaRow> filas);
}
