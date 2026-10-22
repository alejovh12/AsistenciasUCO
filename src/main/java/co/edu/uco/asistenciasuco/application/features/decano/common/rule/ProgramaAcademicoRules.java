package co.edu.uco.asistenciasuco.application.features.decano.common.rule;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import java.util.UUID;

/** Reglas de forma compartidas por los casos de uso del programa academico. */
public final class ProgramaAcademicoRules {

    public static final int NOMBRE_MAXIMO = 50;

    private ProgramaAcademicoRules() {
    }

    public static String nombreNormalizado(final String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ValidationException("El nombre del programa academico es obligatorio.");
        }
        final String normalizado = nombre.trim();
        if (normalizado.length() > NOMBRE_MAXIMO) {
            throw new ValidationException("El nombre del programa academico no puede superar " + NOMBRE_MAXIMO + " caracteres.");
        }
        return normalizado;
    }

    public static void requerido(final Object valor, final String mensaje) {
        if (valor == null) {
            throw new ValidationException(mensaje);
        }
    }

    public static UUID facultadDelDecano(final InstitutionalScopePort scopePort, final UUID actor) {
        return scopePort.findFacultadIdByDecanoUsuario(actor)
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver la facultad del decano autenticado."));
    }
}
