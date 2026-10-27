package co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.usecase;

import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.usecase.domain.ConsultarTendenciaAusentismoDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.usecase.domain.PuntoTendenciaDomain;
import java.util.List;

public interface ConsultarTendenciaAusentismoUseCase {

    List<PuntoTendenciaDomain> execute(ConsultarTendenciaAusentismoDomain domain);
}
