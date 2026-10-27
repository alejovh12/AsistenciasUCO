package co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.usecase;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.usecase.domain.ConsultarKpisCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.usecase.domain.KpisCoordinadorDomain;

public interface ConsultarKpisCoordinadorUseCase {

    KpisCoordinadorDomain execute(ConsultarKpisCoordinadorDomain domain);
}
