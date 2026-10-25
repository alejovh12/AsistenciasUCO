package co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.usecase;

import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.usecase.domain.ConsultarKpisDecanoDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.usecase.domain.KpisDecanoDomain;

public interface ConsultarKpisDecanoUseCase {

    KpisDecanoDomain execute(ConsultarKpisDecanoDomain domain);
}
