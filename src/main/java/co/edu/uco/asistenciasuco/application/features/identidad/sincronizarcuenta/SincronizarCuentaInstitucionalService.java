package co.edu.uco.asistenciasuco.application.features.identidad.sincronizarcuenta;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.UsuarioSincronizacionIdentidadPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.ActualizarCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Sincroniza la cuenta de identidad de un usuario despues de que el cambio ya se confirmo en la base de datos.
 *
 * <p>La base de datos es la fuente de verdad: si el proveedor de identidad falla, no responde, no implementa la
 * operacion o la cuenta no existe, no se propaga la excepcion. Se registra el desfase y se devuelve
 * {@link ResultadoSincronizacion#PENDIENTE}; el control de acceso sigue protegido por el RBAC en DB.</p>
 *
 * <p>POJO sin anotaciones de framework: se cablea con {@code @Bean} en la configuracion de infraestructura.</p>
 */
public final class SincronizarCuentaInstitucionalService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SincronizarCuentaInstitucionalService.class);

    static final String OPERACION_DESACTIVAR = "DESACTIVAR";
    static final String OPERACION_REACTIVAR = "REACTIVAR";
    static final String OPERACION_ACTUALIZAR = "ACTUALIZAR";

    private final IdentityProviderPort identityProvider;
    private final UsuarioSincronizacionIdentidadPort desfases;

    public SincronizarCuentaInstitucionalService(
            final IdentityProviderPort identityProvider,
            final UsuarioSincronizacionIdentidadPort desfases
    ) {
        this.identityProvider = Objects.requireNonNull(identityProvider, "IdentityProviderPort es obligatorio.");
        this.desfases = Objects.requireNonNull(desfases, "UsuarioSincronizacionIdentidadPort es obligatorio.");
    }

    /**
     * Retira el rol desactivado y, solo si la persona no conserva ningun otro rol activo, deshabilita la cuenta.
     */
    public ResultadoSincronizacion desactivar(
            final UUID idUsuario,
            final InstitutionalRole rol,
            final boolean personaSinRolesActivos
    ) {
        return sincronizar(OPERACION_DESACTIVAR, idUsuario, idExterno -> {
            identityProvider.retirarRol(idExterno, rol);
            if (personaSinRolesActivos) {
                identityProvider.deshabilitarCuenta(idExterno);
            }
        });
    }

    /** Habilita la cuenta y reasigna el rol reactivado. */
    public ResultadoSincronizacion reactivar(final UUID idUsuario, final InstitutionalRole rol) {
        return sincronizar(OPERACION_REACTIVAR, idUsuario, idExterno -> {
            identityProvider.habilitarCuenta(idExterno);
            identityProvider.asignarRol(idExterno, rol);
        });
    }

    /** Actualiza correo y nombres de la cuenta. */
    public ResultadoSincronizacion actualizarDatos(
            final UUID idUsuario,
            final ActualizarCuentaIdentidadDTO datos
    ) {
        return sincronizar(OPERACION_ACTUALIZAR, idUsuario,
                idExterno -> identityProvider.actualizarDatosCuenta(idExterno, datos));
    }

    private ResultadoSincronizacion sincronizar(
            final String operacion,
            final UUID idUsuario,
            final Consumer<String> accion
    ) {
        try {
            final Optional<CuentaIdentidadDTO> cuenta = identityProvider.buscarCuentaPorIdUsuario(idUsuario);
            if (cuenta.isEmpty()) {
                registrarDesfase(operacion, idUsuario, "La cuenta de identidad no existe en el proveedor.");
                return ResultadoSincronizacion.PENDIENTE;
            }
            accion.accept(cuenta.get().idExterno());
            return ResultadoSincronizacion.OK;
        } catch (final IdentityProviderPort.IdentityProviderException exception) {
            registrarDesfase(operacion, idUsuario, String.valueOf(exception.getMessage()));
            return ResultadoSincronizacion.PENDIENTE;
        }
    }

    private void registrarDesfase(final String operacion, final UUID idUsuario, final String error) {
        try {
            desfases.registrar(idUsuario, operacion, error);
        } catch (final RuntimeException exception) {
            // Registrar el desfase no puede ocultar ni revertir la operacion principal ya confirmada en DB.
            LOGGER.atError()
                    .addKeyValue("operation", operacion)
                    .addKeyValue("eventType", "IDENTITY_DRIFT_REGISTRATION_FAILED")
                    .setCause(exception)
                    .log("No fue posible registrar el desfase de identidad.");
        }
    }
}
