package co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports;

import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.dto.ConsultarAforoGruposDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.dto.AforoGruposPaginaDTO;

public interface ConsultarAforoGruposInputPort {

    AforoGruposPaginaDTO execute(ConsultarAforoGruposDTO dto);
}
