package co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.usecase;

import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.usecase.domain.CambiarEstadoProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.decano.common.domain.ProgramaAcademicoDomain;

public interface CambiarEstadoProgramaUseCase {

    ProgramaAcademicoDomain execute(CambiarEstadoProgramaDomain domain);
}
