package co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.primaryports;

import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.primaryports.dto.ConsultarPadronGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.primaryports.dto.PadronGrupoPaginaDTO;

/** CIERRE-W1A: padron del grupo del docente titular, paginado y con busqueda en el servidor. */
public interface ConsultarPadronGrupoInputPort {

    PadronGrupoPaginaDTO execute(ConsultarPadronGrupoDTO dto);
}
