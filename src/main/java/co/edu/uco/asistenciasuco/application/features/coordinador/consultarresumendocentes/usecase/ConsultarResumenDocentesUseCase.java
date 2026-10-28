package co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.usecase;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.usecase.domain.ResumenDocentesProgramaDomain;

import java.util.UUID;

public interface ConsultarResumenDocentesUseCase {

    ResumenDocentesProgramaDomain execute(UUID actorUsuarioId);
}
