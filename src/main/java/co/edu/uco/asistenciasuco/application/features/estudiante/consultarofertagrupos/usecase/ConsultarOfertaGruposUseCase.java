package co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.usecase;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.usecase.domain.ConsultarOfertaGruposDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.usecase.domain.OfertaGruposPaginaDomain;

public interface ConsultarOfertaGruposUseCase {

    OfertaGruposPaginaDomain execute(ConsultarOfertaGruposDomain domain);
}
