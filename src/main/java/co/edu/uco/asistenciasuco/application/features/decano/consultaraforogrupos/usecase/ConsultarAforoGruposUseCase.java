package co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.usecase;

import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.usecase.domain.ConsultarAforoGruposDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.usecase.domain.AforoGruposPaginaDomain;

public interface ConsultarAforoGruposUseCase {

    AforoGruposPaginaDomain execute(ConsultarAforoGruposDomain domain);
}
