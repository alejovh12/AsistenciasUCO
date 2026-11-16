package co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.usecase;

import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.usecase.domain.ConsultarPadronGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.usecase.domain.PadronGrupoPaginaDomain;

public interface ConsultarPadronGrupoUseCase {

    PadronGrupoPaginaDomain execute(ConsultarPadronGrupoDomain domain);
}
