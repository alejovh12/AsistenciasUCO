package co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.primaryports;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.primaryports.dto.ResumenDocentesProgramaDTO;

import java.util.UUID;

public interface ConsultarResumenDocentesInputPort {

    ResumenDocentesProgramaDTO execute(UUID actorUsuarioId);
}
