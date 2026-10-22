package co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.usecase;

import co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.usecase.domain.ActualizarDocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.domain.DocenteProgramaDomain;

public interface ActualizarDocenteProgramaUseCase {

    DocenteProgramaDomain execute(ActualizarDocenteProgramaDomain domain);
}
