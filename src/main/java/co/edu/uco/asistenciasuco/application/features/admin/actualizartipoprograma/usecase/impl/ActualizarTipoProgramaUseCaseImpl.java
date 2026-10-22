package co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.usecase.ActualizarTipoProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.usecase.domain.ActualizarTipoProgramaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.TipoProgramaCommandPort;

import java.util.Objects;

public class ActualizarTipoProgramaUseCaseImpl implements ActualizarTipoProgramaUseCase {

    private final TipoProgramaCommandPort tipoProgramaCommandPort;

    public ActualizarTipoProgramaUseCaseImpl(final TipoProgramaCommandPort tipoProgramaCommandPort) {
        this.tipoProgramaCommandPort = Objects.requireNonNull(tipoProgramaCommandPort, "El puerto de comando de tipos de programa es obligatorio.");
    }

    @Override
    public void execute(final ActualizarTipoProgramaDomain domain) {
        if (domain == null) {
            throw new ValidationException("Los datos del tipo de programa son obligatorios.");
        }
        if (domain.id() == null) {
            throw new ValidationException("El identificador del tipo de programa es obligatorio.");
        }
        final String nombre = domain.nombre() != null ? domain.nombre().trim() : "";
        if (nombre.length() < 3 || nombre.length() > 50) {
            throw new ValidationException("El nombre del tipo de programa es obligatorio y debe tener entre 3 y 50 caracteres.");
        }
        tipoProgramaCommandPort.actualizarTipoPrograma(new TipoProgramaCommandPort.ActualizarTipoProgramaCommand(domain.id(), nombre, domain.actorUsuarioId()));
    }
}
