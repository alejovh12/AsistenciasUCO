package co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ResultadoValidacionCargaDTO;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ValidarCargaUsuariosCommand;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.CargaUsuariosValidacionPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.ValidarCargaUsuariosUseCase;

import java.util.Objects;

public class ValidarCargaUsuariosUseCaseImpl implements ValidarCargaUsuariosUseCase {

    private final CargaUsuariosValidacionPort validacionPort;

    public ValidarCargaUsuariosUseCaseImpl(final CargaUsuariosValidacionPort validacionPort) {
        this.validacionPort = Objects.requireNonNull(validacionPort, "CargaUsuariosValidacionPort es obligatorio.");
    }

    @Override
    public ResultadoValidacionCargaDTO execute(final ValidarCargaUsuariosCommand command) {
        throw new UnsupportedOperationException("STUB RED");
    }
}
