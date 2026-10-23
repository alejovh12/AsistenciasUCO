package co.edu.uco.asistenciasuco.application.features.admin.creartipoprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.creartipoprograma.usecase.CrearTipoProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.creartipoprograma.usecase.domain.CrearTipoProgramaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.TipoProgramaCommandPort;

import java.util.Objects;
import java.util.Locale;
import java.util.regex.Pattern;

public class CrearTipoProgramaUseCaseImpl implements CrearTipoProgramaUseCase {

    private static final Pattern CODIGO = Pattern.compile("[A-Z0-9]{3}");

    private final TipoProgramaCommandPort tipoProgramaCommandPort;

    public CrearTipoProgramaUseCaseImpl(final TipoProgramaCommandPort tipoProgramaCommandPort) {
        this.tipoProgramaCommandPort = Objects.requireNonNull(tipoProgramaCommandPort, "El puerto de comando de tipos de programa es obligatorio.");
    }

    @Override
    public void execute(final CrearTipoProgramaDomain domain) {
        if (domain == null) {
            throw new ValidationException("Los datos del tipo de programa son obligatorios.");
        }
        final String codigo = domain.codigo() != null ? domain.codigo().trim().toUpperCase(Locale.ROOT) : "";
        if (!CODIGO.matcher(codigo).matches()) {
            throw new ValidationException("El codigo del tipo de programa es obligatorio y debe tener 3 caracteres alfanumericos.");
        }
        final String nombre = domain.nombre() != null ? domain.nombre().trim() : "";
        if (nombre.length() < 3 || nombre.length() > 50) {
            throw new ValidationException("El nombre del tipo de programa es obligatorio y debe tener entre 3 y 50 caracteres.");
        }
        tipoProgramaCommandPort.crearTipoPrograma(new TipoProgramaCommandPort.CrearTipoProgramaCommand(codigo, nombre, domain.actorUsuarioId()));
    }
}
