package co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.usecase.CambiarEstadoCoordinadorUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.usecase.domain.CambiarEstadoCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.decano.common.domain.ResultadoGestionCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.identidad.sincronizarcuenta.ResultadoSincronizacion;
import co.edu.uco.asistenciasuco.application.features.identidad.sincronizarcuenta.SincronizarCuentaInstitucionalService;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoGestionCoordinadorProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.Objects;
import java.util.UUID;

/**
 * HU122: el decano activa o desactiva a un coordinador de su facultad.
 *
 * <p>La base de datos decide (RBAC, ambito y la regla de no desactivar a quien coordina un programa activo) y
 * responde si la persona se queda sin roles activos. Solo despues se sincroniza la cuenta de identidad; si el
 * proveedor falla se responde con advertencia {@code IAM_002}, porque el RBAC en DB ya bloquea el acceso.</p>
 */
public final class CambiarEstadoCoordinadorUseCaseImpl implements CambiarEstadoCoordinadorUseCase {

    static final String CODIGO_ADVERTENCIA_IDENTIDAD = "IAM_002";

    private final CoordinadorCommandPort commandPort;
    private final InstitutionalScopePort scopePort;
    private final SincronizarCuentaInstitucionalService sincronizacion;
    private final MessageCatalogPort messageCatalog;

    public CambiarEstadoCoordinadorUseCaseImpl(
            final CoordinadorCommandPort commandPort,
            final InstitutionalScopePort scopePort,
            final SincronizarCuentaInstitucionalService sincronizacion,
            final MessageCatalogPort messageCatalog
    ) {
        this.commandPort = Objects.requireNonNull(commandPort, "CoordinadorCommandPort es obligatorio.");
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.sincronizacion = Objects.requireNonNull(sincronizacion, "SincronizarCuentaInstitucionalService es obligatorio.");
        this.messageCatalog = Objects.requireNonNull(messageCatalog, "MessageCatalogPort es obligatorio.");
    }

    @Override
    public ResultadoGestionCoordinadorDomain execute(final CambiarEstadoCoordinadorDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para cambiar el estado del coordinador es obligatorio.");
        }
        final UUID facultad = scopePort.findFacultadIdByDecanoUsuario(domain.usuario())
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver la facultad del decano autenticado."));

        final ResultadoGestionCoordinadorProjection resultado = commandPort.cambiarEstadoCoordinador(
                domain.idCoordinador(), domain.estaActivo(), facultad, domain.usuario());

        final ResultadoSincronizacion sincronizacionResultado = domain.estaActivo()
                ? sincronizacion.reactivar(resultado.idUsuario(), InstitutionalRole.COORDINADOR)
                : sincronizacion.desactivar(resultado.idUsuario(), InstitutionalRole.COORDINADOR,
                        resultado.personaSinRolesActivos());

        return sincronizacionResultado == ResultadoSincronizacion.PENDIENTE
                ? new ResultadoGestionCoordinadorDomain(domain.idCoordinador(), true,
                        messageCatalog.getUserMessage(CODIGO_ADVERTENCIA_IDENTIDAD))
                : new ResultadoGestionCoordinadorDomain(domain.idCoordinador(), false, null);
    }
}
