package co.edu.uco.asistenciasuco.application.secondaryports.identity;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;

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
