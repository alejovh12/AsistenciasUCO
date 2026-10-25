package co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.usecase;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.usecase.domain.AlertasPaginaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.usecase.domain.ConsultarAlertaTempranaDomain;

public interface ConsultarAlertaTempranaUseCase {

    AlertasPaginaDomain execute(ConsultarAlertaTempranaDomain domain);
}
