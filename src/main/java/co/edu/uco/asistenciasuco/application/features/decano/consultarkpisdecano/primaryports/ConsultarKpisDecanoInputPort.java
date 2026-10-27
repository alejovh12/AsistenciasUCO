package co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports;

import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.dto.ConsultarKpisDecanoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.dto.KpisDecanoDTO;

public interface ConsultarKpisDecanoInputPort {

    KpisDecanoDTO execute(ConsultarKpisDecanoDTO dto);
}
