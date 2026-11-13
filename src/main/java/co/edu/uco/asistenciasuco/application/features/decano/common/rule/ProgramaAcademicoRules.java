package co.edu.uco.asistenciasuco.application.features.decano.common.rule;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import java.util.UUID;

/** Reglas de forma compartidas por los casos de uso del programa academico. */
public final class ProgramaAcademicoRules {

    public static final int NOMBRE_MAXIMO = 50;

    private ProgramaAcademicoRules() {
    }

    public static String nombreNormalizado(final String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_045);
        }
        final String normalizado = nombre.trim();
        if (normalizado.length() > NOMBRE_MAXIMO) {
            throw ValidationException.conParametros(MensajeUsuarioErrorCode.VAL_APP_046, NOMBRE_MAXIMO);
        }
        return normalizado;
    }

    public static void requerido(final Object valor, final ErrorDefinition mensaje) {
        if (valor == null) {
            throw new ValidationException(mensaje);
        }
    }

    public static UUID facultadDelDecano(final InstitutionalScopePort scopePort, final UUID actor) {
        return scopePort.findFacultadIdByDecanoUsuario(actor)
                .orElseThrow(() -> new ForbiddenException(MensajeUsuarioErrorCode.ACC_APP_011));
    }
}
