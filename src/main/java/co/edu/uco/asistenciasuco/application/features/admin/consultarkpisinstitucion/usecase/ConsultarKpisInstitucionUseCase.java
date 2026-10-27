package co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.usecase;

import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.usecase.domain.ConsultarKpisInstitucionDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.usecase.domain.KpisInstitucionDomain;

public interface ConsultarKpisInstitucionUseCase {

    KpisInstitucionDomain execute(ConsultarKpisInstitucionDomain domain);
}
