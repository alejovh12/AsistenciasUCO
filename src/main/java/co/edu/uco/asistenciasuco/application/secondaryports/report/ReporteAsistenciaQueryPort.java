package co.edu.uco.asistenciasuco.application.secondaryports.report;

import java.util.List;
import java.util.UUID;

public interface ReporteAsistenciaQueryPort {

    List<ReporteAsistenciaRow> consultarReporteAsistenciaGrupo(UUID grupoId);
}
