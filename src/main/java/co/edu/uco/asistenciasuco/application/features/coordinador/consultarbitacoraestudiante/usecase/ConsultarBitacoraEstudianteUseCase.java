package co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.usecase;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.usecase.domain.BitacoraPaginaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.usecase.domain.ConsultarBitacoraEstudianteDomain;

public interface ConsultarBitacoraEstudianteUseCase {

    BitacoraPaginaDomain execute(ConsultarBitacoraEstudianteDomain domain);
}
