package co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.primaryports;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.primaryports.dto.PeriodoAcademicoDTO;
import java.util.List;

public interface ConsultarPeriodosAcademicosInputPort { List<PeriodoAcademicoDTO> execute();

    /** CIERRE-W2A: con {@code estado} solo los periodos en ese estado (PLANEACION, ACTIVO, CERRADO). */
    List<PeriodoAcademicoDTO> execute(String estado); }
