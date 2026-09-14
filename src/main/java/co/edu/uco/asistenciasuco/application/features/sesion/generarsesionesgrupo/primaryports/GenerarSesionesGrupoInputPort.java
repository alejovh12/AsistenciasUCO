package co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports;

import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.dto.GenerarSesionesGrupoDTO;

public interface GenerarSesionesGrupoInputPort {

    void execute(GenerarSesionesGrupoDTO dto);
}
