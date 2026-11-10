package co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase;

import co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.domain.CrearDecanoDomain;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.domain.CrearDecanoResultado;

public interface CrearDecanoUseCase {
    CrearDecanoResultado execute(CrearDecanoDomain domain);
}
