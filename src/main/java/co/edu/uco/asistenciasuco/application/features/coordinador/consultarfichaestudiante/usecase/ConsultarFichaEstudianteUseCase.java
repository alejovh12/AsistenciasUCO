package co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.usecase;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.usecase.domain.ConsultarFichaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.usecase.domain.FichaEstudianteDomain;

public interface ConsultarFichaEstudianteUseCase {

    FichaEstudianteDomain execute(ConsultarFichaEstudianteDomain domain);
}
