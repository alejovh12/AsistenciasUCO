package co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.usecase;

import co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.usecase.domain.CambiarEstadoDocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.domain.DocenteProgramaDomain;

public interface CambiarEstadoDocenteProgramaUseCase {

    DocenteProgramaDomain execute(CambiarEstadoDocenteProgramaDomain domain);
}
