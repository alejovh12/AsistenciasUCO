package co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.usecase;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.usecase.domain.ConsultarReclamosEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.usecase.domain.ReclamosEstudiantePaginaDomain;

public interface ConsultarReclamosEstudianteUseCase {

    ReclamosEstudiantePaginaDomain execute(ConsultarReclamosEstudianteDomain domain);
}
