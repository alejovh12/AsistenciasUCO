package co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase;

import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.domain.ConsultarProgramasDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.domain.ProgramasPaginaDomain;

public interface ConsultarProgramasUseCase {

    ProgramasPaginaDomain execute(ConsultarProgramasDomain domain);
}
