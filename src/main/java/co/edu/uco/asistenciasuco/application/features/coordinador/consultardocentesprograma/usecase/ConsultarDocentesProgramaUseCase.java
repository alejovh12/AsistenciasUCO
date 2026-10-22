package co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.domain.DocentesProgramaPaginaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.domain.ConsultarDocentesProgramaDomain;

import java.util.UUID;

public interface ConsultarDocentesProgramaUseCase {

    DocentesProgramaPaginaDomain execute(UUID actorUsuarioId, ConsultarDocentesProgramaDomain consulta);
}
