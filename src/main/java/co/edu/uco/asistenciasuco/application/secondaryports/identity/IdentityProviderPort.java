package co.edu.uco.asistenciasuco.application.secondaryports.identity;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;

/**
 * Puerto secundario para la gestión de cuentas en el Proveedor de Identidad (IdP) institucional.
 *
 * <p>Este puerto es completamente agnóstico al IdP subyacente. Su implementación concreta
 * puede ser Keycloak, Auth0, AWS Cognito, un desarrollo propio u otro sistema de autenticación.
 * Los controllers y casos de uso solo conocen esta interfaz.</p>
 *
 * <p>Para cambiar de proveedor de identidad:
 * <ol>
 *   <li>Crear una nueva clase que implemente {@code IdentityProviderPort}.</li>
 *   <li>Registrarla como bean en {@code IdentityProviderBeansConfig}.</li>
 *   <li>No se requiere ningún otro cambio en la aplicación.</li>
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
     * Elimina la cuenta del usuario en el IdP. Se usa principalmente como mecanismo de
     * compensación cuando la creación en la base de datos falla después de haber
     * creado exitosamente la cuenta en el IdP.
     *
     * @param idExterno ID asignado por el IdP al usuario (retornado por {@link #crearCuenta}).
     */
    void eliminarCuenta(String idExterno);

    /**
     * Asigna un rol institucional a un usuario ya existente en el IdP.
     *
     * @param idExterno    ID del usuario en el IdP.
     * @param nombreRol    Nombre del rol a asignar (ej: "docente", "coordinador", "decano").
     */
    void asignarRol(String idExterno, String nombreRol);

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
