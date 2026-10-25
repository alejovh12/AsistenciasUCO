package co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.dto.ConsultarTendenciaAusentismoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.dto.PuntoTendenciaDTO;
import java.util.List;

public interface ConsultarTendenciaAusentismoInputPort {

    List<PuntoTendenciaDTO> execute(ConsultarTendenciaAusentismoDTO dto);
}
