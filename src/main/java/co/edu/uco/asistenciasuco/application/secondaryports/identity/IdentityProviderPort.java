package co.edu.uco.asistenciasuco.application.secondaryports.identity;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.ActualizarCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto secundario para la gestión de cuentas en el Proveedor de Identidad (IdP) institucional.
 *
 * <p>Este puerto es completamente agnóstico al IdP subyacente. Su implementación concreta
 * puede ser cualquier sistema de autenticación externo o propio. Los controllers y casos de
 * uso solo conocen esta interfaz.</p>
 *
 * <p>Para cambiar de proveedor de identidad:
 * <ol>
 *   <li>Crear una nueva clase que implemente {@code IdentityProviderPort}.</li>
 *   <li>Registrarla en una configuración del Composition Root condicionada a
 *       {@code app.adapters.identity.provider}.</li>
 *   <li>No se requiere ningún otro cambio en Domain, Application, UseCase ni Controller.</li>
 * </ol>
 * </p>
 */
public interface IdentityProviderPort {

    /**
     * Crea la cuenta del usuario en el IdP y le asigna el rol institucional indicado.
     * Si el usuario ya existe en el IdP, retorna su información sin lanzar excepción.
     *
     * @param dto Datos necesarios para crear la cuenta.
     * @return Resultado con el {@code idExterno} asignado por el IdP.
     * @throws IdentityProviderException Si ocurre un error de comunicación o de negocio con el IdP.
     */
    CuentaIdentidadDTO crearCuenta(CrearCuentaIdentidadDTO dto);

    /**
     * Elimina la cuenta del usuario en el IdP. Es un mecanismo de compensación
     * <b>best-effort</b> para una identidad creada durante la misma operación de
     * {@link #crearCuenta}, cuando un paso posterior de esa misma operación falla (ej. no se
     * pudo establecer el password o asignar el rol inmediatamente después de crear la cuenta).
     *
     * <p><b>No</b> es un mecanismo de rollback distribuido entre la base de datos institucional
     * y el IdP: si una operación de base de datos que ya hizo commit falla en un paso posterior
     * de Identity, este método no debe usarse para deshacer esa operación de DB, ni a la
     * inversa. La reconciliación distribuida DB↔IdP es una capability de una fase posterior.</p>
     *
     * @param idExterno ID asignado por el IdP al usuario (retornado por {@link #crearCuenta}).
     */
    void eliminarCuenta(String idExterno);

    /**
     * Asigna un rol institucional a un usuario ya existente en el IdP.
     *
     * @param idExterno ID del usuario en el IdP.
     * @param rol       Rol institucional a asignar. Application nunca envía el rol como texto
     *                  libre: el adapter traduce este enum al nombre exacto del client role del
     *                  IdP.
     */
    void asignarRol(String idExterno, InstitutionalRole rol);

    // -------------------------------------------------------------------------
    // Ciclo de vida de la cuenta (HU121/HU122, IAM-001). Capacidades OPCIONALES del proveedor: un proveedor que
    // no las implemente falla de forma controlada con IdentityProviderException, que la sincronizacion trata como
    // desfase pendiente (la base de datos sigue siendo la fuente de verdad y el RBAC en DB ya protege el acceso).
    // -------------------------------------------------------------------------

    /**
     * Busca la cuenta por el vinculo institucional estable ({@code Usuario.id}), nunca por correo ni username.
     *
     * @return la cuenta si hay una coincidencia exacta; vacio si no existe.
     * @throws IdentityProviderException si la identidad es ambigua (mas de una coincidencia) o el IdP falla.
     */
    default Optional<CuentaIdentidadDTO> buscarCuentaPorIdUsuario(final UUID idUsuario) {
        throw operacionNoSoportada();
    }

    /** Deshabilita la cuenta (no puede iniciar sesion) y cierra sus sesiones. Idempotente. */
    default void deshabilitarCuenta(final String idExterno) {
        throw operacionNoSoportada();
    }

    /** Habilita la cuenta. Idempotente. */
    default void habilitarCuenta(final String idExterno) {
        throw operacionNoSoportada();
    }

    /** Actualiza correo y nombres de la cuenta. No cambia el nombre de usuario ni la contrasena. */
    default void actualizarDatosCuenta(final String idExterno, final ActualizarCuentaIdentidadDTO datos) {
        throw operacionNoSoportada();
    }

    /**
     * IDENT-001: confirma el correo de la cuenta (habilita la cuenta y marca el correo verificado). Idempotente.
     * Capacidad opcional del proveedor: si no la implementa falla de forma controlada.
     */
    default void confirmarCorreoCuenta(final String idExterno) {
        throw operacionNoSoportada();
    }

    /** Retira un rol institucional de la cuenta. Idempotente: si no lo tiene, no falla. */
    default void retirarRol(final String idExterno, final InstitutionalRole rol) {
        throw operacionNoSoportada();
    }

    /** Correo con el que la cuenta existe en el proveedor (identifica a la persona frente al motor de autorización). */
    default Optional<String> correoDeCuenta(final String idExterno) {
        throw operacionNoSoportada();
    }

    /** Roles institucionales que la cuenta tiene asignados en el proveedor. */
    default java.util.Set<InstitutionalRole> rolesDeCuenta(final String idExterno) {
        throw operacionNoSoportada();
    }

    private static IdentityProviderException operacionNoSoportada() {
        return new IdentityProviderException("Operacion no soportada por este proveedor de identidad.");
    }

    /**
     * Excepción lanzada cuando ocurre un error al comunicarse con el proveedor de identidad.
     */
    final class IdentityProviderException extends RuntimeException {

        public IdentityProviderException(final String message) {
            super(message);
        }

        public IdentityProviderException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
