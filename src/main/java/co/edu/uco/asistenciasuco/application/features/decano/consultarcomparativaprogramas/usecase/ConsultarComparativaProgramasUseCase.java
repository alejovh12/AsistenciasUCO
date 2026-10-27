package co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.usecase;

import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.usecase.domain.ConsultarComparativaProgramasDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.usecase.domain.ComparativaProgramaDomain;
import java.util.List;

public interface ConsultarComparativaProgramasUseCase {

    List<ComparativaProgramaDomain> execute(ConsultarComparativaProgramasDomain domain);
}
