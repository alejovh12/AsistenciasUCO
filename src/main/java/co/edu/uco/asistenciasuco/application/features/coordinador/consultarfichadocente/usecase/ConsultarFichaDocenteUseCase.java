package co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.usecase;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.usecase.domain.ConsultarFichaDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.usecase.domain.FichaDocenteDomain;

public interface ConsultarFichaDocenteUseCase {

    FichaDocenteDomain execute(ConsultarFichaDocenteDomain domain);
}
