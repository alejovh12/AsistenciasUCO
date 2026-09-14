package co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.usecase;

import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.usecase.domain.DecanoDomain;

import java.util.List;

public interface ConsultarDecanosUseCase {
    List<DecanoDomain> execute();
}
