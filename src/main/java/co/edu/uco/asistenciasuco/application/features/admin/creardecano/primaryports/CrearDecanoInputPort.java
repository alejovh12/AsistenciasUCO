package co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.dto.CrearDecanoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.dto.CrearDecanoResultadoDTO;

public interface CrearDecanoInputPort {
    CrearDecanoResultadoDTO execute(CrearDecanoDTO dto);
}
