package co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.usecase;

import co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.usecase.domain.ActualizarSemestreEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.usecase.domain.ResultadoSemestreEstudianteDomain;

public interface ActualizarSemestreEstudianteUseCase {

    ResultadoSemestreEstudianteDomain execute(ActualizarSemestreEstudianteDomain domain);
}
