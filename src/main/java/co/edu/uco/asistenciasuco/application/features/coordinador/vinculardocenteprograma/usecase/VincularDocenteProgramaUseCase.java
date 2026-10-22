package co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.usecase;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.domain.DocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.usecase.domain.VincularDocenteProgramaDomain;

public interface VincularDocenteProgramaUseCase {

    DocenteProgramaDomain execute(VincularDocenteProgramaDomain domain);
}
