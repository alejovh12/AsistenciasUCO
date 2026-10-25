package co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.dto.ConsultarKpisInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.dto.KpisInstitucionDTO;

public interface ConsultarKpisInstitucionInputPort {

    KpisInstitucionDTO execute(ConsultarKpisInstitucionDTO dto);
}
