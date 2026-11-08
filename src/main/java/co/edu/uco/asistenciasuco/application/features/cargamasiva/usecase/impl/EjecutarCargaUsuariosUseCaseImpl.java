package co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.CrearDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.EjecutarCargaUsuariosCommand;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ResultadoCargaDTO;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.CargaUsuariosValidacionPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.EjecutarCargaUsuariosUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.primaryports.VincularDocenteProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.CrearCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.AsegurarCuentaIdentidadService;

import java.util.Objects;

public class EjecutarCargaUsuariosUseCaseImpl implements EjecutarCargaUsuariosUseCase {

    private final CargaUsuariosValidacionPort validacionPort;
    private final VincularDocenteProgramaInputPort vincularDocentePort;
    private final CrearCoordinadorInputPort crearCoordinadorPort;
    private final CrearDecanoInputPort crearDecanoPort;
    private final AsegurarCuentaIdentidadService asegurarCuentaService;

    public EjecutarCargaUsuariosUseCaseImpl(
            final CargaUsuariosValidacionPort validacionPort,
            final VincularDocenteProgramaInputPort vincularDocentePort,
            final CrearCoordinadorInputPort crearCoordinadorPort,
            final CrearDecanoInputPort crearDecanoPort,
            final AsegurarCuentaIdentidadService asegurarCuentaService
    ) {
        this.validacionPort = Objects.requireNonNull(validacionPort, "CargaUsuariosValidacionPort es obligatorio.");
        this.vincularDocentePort = Objects.requireNonNull(vincularDocentePort, "VincularDocenteProgramaInputPort es obligatorio.");
        this.crearCoordinadorPort = Objects.requireNonNull(crearCoordinadorPort, "CrearCoordinadorInputPort es obligatorio.");
        this.crearDecanoPort = Objects.requireNonNull(crearDecanoPort, "CrearDecanoInputPort es obligatorio.");
        this.asegurarCuentaService = Objects.requireNonNull(asegurarCuentaService, "AsegurarCuentaIdentidadService es obligatorio.");
    }

    @Override
    public ResultadoCargaDTO execute(final EjecutarCargaUsuariosCommand command) {
        throw new UnsupportedOperationException("STUB RED");
    }
}
