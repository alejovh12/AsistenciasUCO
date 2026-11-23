package co.edu.uco.asistenciasuco.application.features.decano.common.rule;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import java.util.UUID;

/** Reglas de ambito compartidas por los casos de uso de KPIs del decano. El calculo no vive aqui: esta en la DB. */
public final class DecanoIndicadoresRules {

    private DecanoIndicadoresRules() {
    }

    public static UUID facultadDelDecano(final InstitutionalScopePort scopePort, final UUID actor) {
        return scopePort.findFacultadIdByDecanoUsuario(actor)
                .orElseThrow(() -> new ForbiddenException(MensajeUsuarioErrorCode.KPI_003));
    }
}
