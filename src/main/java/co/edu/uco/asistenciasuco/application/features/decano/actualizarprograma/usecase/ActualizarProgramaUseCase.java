package co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.usecase;

import co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.usecase.domain.ActualizarProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.decano.common.domain.ProgramaAcademicoDomain;

public interface ActualizarProgramaUseCase {

    ProgramaAcademicoDomain execute(ActualizarProgramaDomain domain);
}
