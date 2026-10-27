package co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.usecase;

import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.usecase.domain.ConsultarComparativaFacultadesDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.usecase.domain.ComparativaFacultadDomain;
import java.util.List;

public interface ConsultarComparativaFacultadesUseCase {

    List<ComparativaFacultadDomain> execute(ConsultarComparativaFacultadesDomain domain);
}
