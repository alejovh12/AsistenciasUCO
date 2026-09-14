package co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.primaryports;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.PeriodoAcademicoDTO;
import java.util.List;

public interface ConsultarPeriodosAcademicosInputPort { List<PeriodoAcademicoDTO> execute(); }
