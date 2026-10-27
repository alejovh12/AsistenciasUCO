package co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.primaryports;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.primaryports.dto.ConsultarOfertaGruposDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.primaryports.dto.OfertaGruposPaginaDTO;

public interface ConsultarOfertaGruposInputPort {

    OfertaGruposPaginaDTO execute(ConsultarOfertaGruposDTO dto);
}
