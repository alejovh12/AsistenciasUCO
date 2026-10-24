package co.edu.uco.asistenciasuco.application.features.admin.creararea.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.creararea.usecase.CrearAreaUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.creararea.usecase.domain.CrearAreaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AreaCommandPort;

import java.util.Objects;
import java.util.Locale;
import java.util.regex.Pattern;

public class CrearAreaUseCaseImpl implements CrearAreaUseCase {

    private static final Pattern CODIGO = Pattern.compile("[A-Z0-9]{3}");

    private final AreaCommandPort areaCommandPort;

    public CrearAreaUseCaseImpl(final AreaCommandPort areaCommandPort) {
        this.areaCommandPort = Objects.requireNonNull(areaCommandPort, "El puerto de comando de areas es obligatorio.");
    }

    @Override
    public void execute(final CrearAreaDomain domain) {
        if (domain == null) {
            throw new ValidationException("Los datos del area de conocimiento son obligatorios.");
        }
        final String codigo = domain.codigo() != null ? domain.codigo().trim().toUpperCase(Locale.ROOT) : "";
        if (!CODIGO.matcher(codigo).matches()) {
            throw new ValidationException("El codigo del area de conocimiento es obligatorio y debe tener 3 caracteres alfanumericos.");
        }
        final String nombre = domain.nombre() != null ? domain.nombre().trim() : "";
        if (nombre.length() < 3 || nombre.length() > 50) {
            throw new ValidationException("El nombre del area de conocimiento es obligatorio y debe tener entre 3 y 50 caracteres.");
        }
        areaCommandPort.crearArea(new AreaCommandPort.CrearAreaCommand(codigo, nombre, domain.actorUsuarioId()));
    }
}
