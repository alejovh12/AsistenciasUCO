package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.usecase.CambiarEstadoTipoProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.usecase.domain.CambiarEstadoTipoProgramaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.TipoProgramaCommandPort;

import java.util.Objects;

public class CambiarEstadoTipoProgramaUseCaseImpl implements CambiarEstadoTipoProgramaUseCase {

    private final TipoProgramaCommandPort tipoProgramaCommandPort;

    public CambiarEstadoTipoProgramaUseCaseImpl(final TipoProgramaCommandPort tipoProgramaCommandPort) {
        this.tipoProgramaCommandPort = Objects.requireNonNull(tipoProgramaCommandPort, "El puerto de comando de tipos de programa es obligatorio.");
    }

    @Override
    public void execute(final CambiarEstadoTipoProgramaDomain domain) {
        if (domain == null) {
            throw new ValidationException("Los datos del tipo de programa son obligatorios.");
        }
        if (domain.id() == null) {
            throw new ValidationException("El identificador del tipo de programa es obligatorio.");
        }
        if (domain.activo() == null) {
            throw new ValidationException("El estado deseado del tipo de programa es obligatorio.");
        }
        tipoProgramaCommandPort.cambiarEstadoTipoPrograma(new TipoProgramaCommandPort.CambiarEstadoTipoProgramaCommand(domain.id(), domain.activo(), domain.actorUsuarioId()));
    }
}
