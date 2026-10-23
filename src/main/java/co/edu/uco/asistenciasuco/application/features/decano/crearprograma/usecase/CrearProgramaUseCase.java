package co.edu.uco.asistenciasuco.application.features.decano.crearprograma.usecase;

import co.edu.uco.asistenciasuco.application.features.decano.common.domain.ProgramaAcademicoDomain;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.usecase.domain.CrearProgramaDomain;

public interface CrearProgramaUseCase {

    ProgramaAcademicoDomain execute(CrearProgramaDomain domain);
}
